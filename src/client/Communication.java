package client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Communication implements Runnable {
    private Thread game_thread;
    private AtomicBoolean interrupt;
    public AtomicInteger allow_op;
    public BlockingQueue<String> tcp_queue_in, tcp_queue_out;
    public Selector selector;
    private SelectionKey tcp_key;

    //volatile perché l'inserimento dinamico è balordo
    private volatile SelectionKey udp_key;

    public class TCPAttachment {
        public int status;
        public ByteBuffer buffer;

        public TCPAttachment(ByteBuffer buffer){
            status = 0;
            this.buffer = buffer;
        }
    }

    public Communication(SocketChannel tcp_sock, Thread game_thread, AtomicBoolean interrupt){
        this.game_thread = game_thread;

        //flag per bloccare l'input da stdin
        this.interrupt = interrupt;

        //flag per impedire al socketChannel di iniziare operazioni dopo le notifiche
        // 0 = non c'è niente a mezzo
        // 1 = è arrivata la notifica e non accetto nuovi invii
        // 2 = ho un invio iniziato che va concluso, non implica l'arrivo della notifica
        // 3 = entrambi
        allow_op = new AtomicInteger(0);

        //blockingQueue utilizzata per inviare i payload da GameClient a Communication
        //  ed inviare le risposte da Communication aGameClient
        this.tcp_queue_in = new LinkedBlockingQueue<String>();
        this.tcp_queue_out = new LinkedBlockingQueue<String>();

        //inizializzo
        try{
            selector = Selector.open();
        }catch (IOException e) {
            System.err.println("#### Problemi d'instaurazione della connessione, operazione annullata");
            //trovare il modo di comunicarlo al mainThread
            //tbd
        }

        udp_key = null;
        tcp_key = null;

        try {
            //inserisco nel selettore i socket in modalità non bloccante
            tcp_sock.configureBlocking(false);
            //solo in read perché l'os da sempre il via libera per la write
            tcp_key = tcp_sock.register(selector, SelectionKey.OP_READ);
        } catch (IOException e) {
            System.err.println("#### Problemi d'instaurazione della connessione, operazione annullata");
            //trovare il modo di comunicarlo al mainThread
            //tbd
        }
    }

    @Override
    public void run() {
        //loop di select del server
        try{
            while (true) {
                if(!tcp_queue_in.isEmpty()){
                    tcp_key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                }

                //provo a fare la select bloccante
                //all'inserimento di messaggi in tcp_queue_in devo fare selector.wakeup che sennò
                //  non se ne accorge.
                try {
                    selector.select();
                }catch (IOException ex) {
                    ex.printStackTrace();
                    break;
                }

                //ottengo il SelectedKeySet
                Set<SelectionKey> readyKeys = selector.selectedKeys();

                //garantisco che se c'è una notifica la stampo subito a schermo.

                if (udp_key != null  && readyKeys.contains(udp_key)) {
                    //controllo per sicurezza
                    if(udp_key.isReadable()){
                        receive_udp(udp_key);
                    }
                    readyKeys.remove(udp_key);
                }

                //per controllare che non ci sia anche il socketChannel in attesa
                if(readyKeys.contains(tcp_key)){
                    handle_tcp(tcp_key);
                    readyKeys.remove(tcp_key);
                }
            }
        }catch (ClosedSelectorException ex) {
            // il selettore è stato chiuso da game.reset()
        }

        //terminazione delle risorse del thread
        try{
            tcp_key.channel().close();
        }catch(IOException e){
            System.err.println("#### Problemi di IO alla chiusura della connessione");
        }

        try{
            if(udp_key != null){
                udp_key.channel().close();
            }
        }catch(IOException e){
            System.err.println("#### Problemi di IO alla chiusura della connessione");
        }

        tcp_queue_out.clear();
        tcp_queue_in.clear();

    }

    public boolean add_udp_channel(DatagramChannel udp_sock){
        try{
            if(udp_sock != null) {
                //ci sono delle operazioni (register e updateCredentials) che vogliono solo il tcp
                // e subito dopo il primo messaggio terminano il thread quindi non inizializzo udp.

                udp_sock.configureBlocking(false);
                selector.wakeup();
                udp_key = udp_sock.register(selector, SelectionKey.OP_READ);

                //mi porto dietro il bytebuffer
                udp_key.attach(ByteBuffer.allocate(4096));
                return true;
            }
        } catch (IOException e) {
        System.err.println("#### Problemi d'instaurazione della connessione, operazione annullata");
            //trovare il modo di comunicarlo al mainThread
            //tbd
        }
        return false;
    }

    private void receive_udp(SelectionKey key){
        try {
            DatagramChannel udp_sock = (DatagramChannel) key.channel();

            //prendo il buffer, non lo sto a riallocare ogni volta
            ByteBuffer buffer = (ByteBuffer) key.attachment();

            //preparo la scrittura
            buffer.clear();

            udp_sock.receive(buffer);

            //preparo la lettura
            buffer.flip();

            // Decodifica direttamente il buffer in una stringa
            String response = StandardCharsets.UTF_8.decode(buffer).toString();
            String messaggio = ClientJsonUtils.get_description(response, "udpPuzzleTermination");

            if(messaggio != null){
                System.out.println(messaggio + "\n***Operazioni in sospeso annullate");

                interrupt.set(true);
                game_thread.interrupt();

                if(allow_op.get() == 0){
                    allow_op.set(1);
                }else if(allow_op.get() == 2){
                    allow_op.set(3);
                }
            }

        }catch (IOException e){
            System.err.println("Errore di IO alla ricezione di una notifica");
        }
    }

    private void handle_tcp(SelectionKey key){
        try{
            SocketChannel sock = (SocketChannel) key.channel();

            if(key.isReadable()){
                //estraggo il messaggio dalla queue se non è già stato letto
                TCPAttachment att = (TCPAttachment) key.attachment();

                if(att == null){
                    //ricevo la risposta, prima però devo sapere quanto è lunga in bytes
                    ByteBuffer buffer = ByteBuffer.allocate(4);
                    att = new TCPAttachment(buffer);
                    key.attach(att);
                }

                if(att.buffer.hasRemaining()){
                    if(sock.read(att.buffer) == -1){
                        throw new IOException("Connessione chiusa dal server prematuramente");
                    }
                }
                if(!att.buffer.hasRemaining()){
                    //fine lettura
                    if(att.status == 0){
                        //vuol dire che ho letto l'intero che mi da la dim del messaggio
                        att.buffer.flip();
                        int dim = att.buffer.getInt();

                        //metto il nuovo buffer allocato appositamente e poi cambio lo stato alla lettura del payload
                        att.buffer = ByteBuffer.allocate(dim);
                        att.status = 1;

                    } else if (att.status == 1) {
                        //vuol dire che ho finito di leggere l'effettivo payload della risposta tcp
                        att.buffer.flip();
                        String response_msg = new String(att.buffer.array(), StandardCharsets.UTF_8);
                        key.attach(null);

                        //uso la stessa queue in input come output
                        tcp_queue_out.offer(response_msg);

                        //ho finito l'operazione a mezzo
                        allow_op.set(0);
                        key.attach(null);
                    }
                }
            }
            else if(key.isWritable()){
                if(allow_op.get() == 1){
                    //messaggio da non mandare
                    tcp_queue_in.clear();
                    key.interestOps(SelectionKey.OP_READ);
                    return;
                }

                //estraggo il messaggio dalla queue se non è già stato letto
                TCPAttachment att = (TCPAttachment) key.attachment();
                ByteBuffer buffer = null;
                if(att != null){
                     buffer = att.buffer;
                }

                String msg = "";

                //ho agito prima dell'arrivo della notifica
                allow_op.set(2);

                //prima scrittura
                if(buffer == null){

                    msg = tcp_queue_in.poll();
                    if(msg == null){
                        System.err.print("Perso un messaggio da inviare");
                        key.interestOps(SelectionKey.OP_READ);
                        return;
                    }


                    //converto in byte la jsonstring per wrapparlo in un ByteBuffer
                    byte[] jsonBytes = msg.getBytes(StandardCharsets.UTF_8);

                    //mi serve un intero per dire al server da quanti byte è composto il payload
                    buffer = ByteBuffer.allocate(4 + jsonBytes.length);
                    buffer.putInt(jsonBytes.length);
                    buffer.put(jsonBytes);

                    //preparo la lettura del buffer per l'ivio dei dati
                    buffer.flip();

                    //lo metto nella key per le successive scritture
                    key.attach(new TCPAttachment(buffer));
                }

                //invio
                if(buffer.hasRemaining()){
                    sock.write(buffer);
                }
                if(!buffer.hasRemaining()){
                    //ho finito di scrivere
                    key.interestOps(SelectionKey.OP_READ);

                    //levo il buffer dall'attachment
                    key.attach(null);
                }
            }

        }catch (IOException e) {
            System.err.println("###\tErrore di IO durante la comunicazione al server" + e);
            System.err.println("### Operazione annullata");

            //vedi come comunicare il problema al main_thread
            game_thread.interrupt();
        }
    }
}
