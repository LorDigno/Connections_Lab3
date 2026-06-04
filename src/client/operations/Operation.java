package client.operations;

import client.GameClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public abstract class Operation{
    public GameClient game;
    public String name;

    //chiama gli altri metodi
    public void execute() throws InterruptedException{
        boolean n = checks();
        if(!n){
            return; //sono falliti dei check
        }

        String msg = payload();

        String response = communicate(msg);
        if (response == null){
            on_fail();
            return;
        }

        digest(response);
    };

    //controlli necessari al fare l'operazione
    public abstract boolean checks();

    //creazione del payload della richiesta
    public abstract String payload() throws InterruptedException;

    //invia msg e rende la risposta relativa
    private String communicate(String msg) throws InterruptedException{

        //dove andrò a immettere la risposta
        String response_msg = "";

        //comunicazione col server via NIO
        try{
            SocketChannel sock = game.tcp_sock;

            //converto in byte la jsonstring per wrapparlo in un ByteBuffer
            byte[] jsonBytes = msg.getBytes(StandardCharsets.UTF_8);

            //mi serve un intero per dire al server da quanti byte è composto il payload
            ByteBuffer out_buffer = ByteBuffer.allocate(4 + jsonBytes.length);
            out_buffer.putInt(jsonBytes.length);
            out_buffer.put(jsonBytes);
            out_buffer.flip();

            //invio
            while(out_buffer.hasRemaining()){
                sock.write(out_buffer);
            }

            //ricevo la risposta, prima però devo sapere quanto è lunga in bytes
            ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
            while (lengthBuffer.hasRemaining()) {
                if (sock.read(lengthBuffer) == -1) {
                    throw new IOException("Connessione chiusa dal server prematuramente");
                }
            }
            lengthBuffer.flip();
            int dim = lengthBuffer.getInt();

            //così alloco il buffer di dimensione giusta
            ByteBuffer in_buffer = ByteBuffer.allocate(dim);

            //ricevo la risposta
            while (in_buffer.hasRemaining()) {
                if (sock.read(in_buffer) == -1) {
                    throw new IOException("Connessione chiusa dal server prematuramente");
                }
            }

            //converto il buffer in stringa utilizzando l'array sottostante
            in_buffer.flip();
            response_msg = new String(in_buffer.array(), StandardCharsets.UTF_8);
        }catch(ClosedByInterruptException e){
            //ho interrotto mentre aspettavo sul socket, questo lo invalida completamente
            System.err.println("###\tInvalidata la connessione col server, logout forzato");
            game.reset();

            //propoago l'interruzione al GameClient
            throw new InterruptedException();

        } catch (IOException e) {
            System.err.println("###\tErrore di IO durante la comunicazione al server" + e);
            System.err.println("### Operazione annullata");
            return null;
        }

        return response_msg;
    }

    //nel caso alcune implementazioni necessitino di un "finally" dopo communicate
    public abstract void on_fail();

    //analisi della risposta e stampa dei risultati a schermo
    public abstract void digest(String response);

    //instaura una connessione tcp col server del GameClient
    protected SocketChannel connessione(){
        //provo ad instaurare la connessione al server
        int port = game.port;
        String host = game.server_host;
        SocketChannel sock = null;
        try{
            sock = SocketChannel.open();
            InetSocketAddress socketAddress = new InetSocketAddress(host, port);
            //Avvio la connessione
            sock.connect(socketAddress);
            //specifico il timeout per le successive operazioni
            sock.socket().setSoTimeout(game.timeout);

        }catch (UnknownHostException e){
            System.err.println("###\tProblemi di risoluzione dns del server" + e);
            return null;
        }catch (IOException e) {
            System.err.println("###\tConnessione rifiutata dal server o andata in timeout" + e);
            return null;
        }

        return sock;
    }

    protected int get_int(String richiesta) throws InterruptedException{
        String in = "";
        int num;
        while(true){
            in = get_string(richiesta);
            try{
                num = Integer.parseInt(in);
                break;
            }catch (NumberFormatException e){
                System.out.print("---Va inserito un numero intero...\n");
            }
        }

        return num;
    }

    protected String get_string(String richiesta) throws InterruptedException{
        String in = "";
        while(true){
            System.out.print(richiesta);
            in = game.get_input();
            if(!in.equals("")){
                return in;
            }

            System.out.print("---Capita di premere troppo invio...\n");
        }
    }

}
