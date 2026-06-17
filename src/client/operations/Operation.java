package client.operations;

import client.ClientCommsException;
import client.Communication;
import client.GameClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeUnit;

public abstract class Operation{
    public GameClient game;
    public String name;

    ///Template Method in cui checks, payload e digest cambiano in base all'operazione
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

    ///Controlli necessari al fare l'operazione
    public abstract boolean checks();

    ///Creazione del payload della richiesta
    public abstract String payload() throws InterruptedException;

    ///Invia msg e rende la risposta relativa
    private String communicate(String msg) throws InterruptedException{

        //dove andrò a immettere la risposta
        String response_msg = null;
        Communication comm_thread = game.comm;

        //comunicazione col server via NIO
        try{
            if(comm_thread.allow_op.get() % 2 == 0) {
                //sono negli stati corretti, 0 o 2
                comm_thread.tcp_queue_in.offer(msg);
                comm_thread.selector.wakeup();
                response_msg = comm_thread.tcp_queue_out.take();
            }
            else{
                throw new InterruptedException("Interruzione durante la comunicazione");
            }
        } catch(InterruptedException e){
            //è arrivata la notifica asincrona oppure il server ha chiuso il socket tcp
            //  controllo che caso è basandomi su interrupted = reject_input

            if(game.reject_input.get()){
                //se è true vuol dire che è arrivata la notifica
                String ret = null;
                if(comm_thread.allow_op.get() == 3){
                    //vuol dire che devo terminare un'operazione lasciata a mezzo ma avviata prima
                    ret = comm_thread.tcp_queue_out.poll(game.timeout, TimeUnit.MILLISECONDS);
                }

                //espando l'interruzione GameClient
                Thread.currentThread().interrupt();
                return ret;
            }
            else{
                //qua c'è stato l'errore nel server, va chiuso tutto
                System.err.println("Il server si è chiuso o ha riscontrato un errore" + e.getMessage());
                return null;
            }
        }

        return response_msg;
    }

    ///Se va storto qualcosa resetto il client
    public void on_fail(){
        game.reset();
    };

    ///Analisi della risposta e stampa dei risultati a schermo
    public abstract void digest(String response);

    ///Instaura una connessione tcp col server del GameClient e avvia il thread di comunicazione
    protected boolean connessione(){
        //provo ad instaurare la connessione al server
        int port = game.port;
        String host = game.server_host;
        SocketChannel sock = null;
        try{
            sock = SocketChannel.open();
            InetSocketAddress socketAddress = new InetSocketAddress(host, port);
            //Avvio la connessione
            sock.connect(socketAddress);
        }catch(UnknownHostException e){
            System.err.println("###\tProblemi di risoluzione dns del server" + e);
            return false;
        }catch(IOException e) {
            System.err.println("###\tConnessione rifiutata dal server o andata in timeout" + e);
            return false;
        }

        //se sono qua il socket tcp è stato instanziato.
        //va inizializzato il thread di comunicazione, per ora senza DatagramChannel associato.
        //sarà dopo,se il login ha successo, ad essere aggiunto.
        try{
            Communication comm = new Communication(sock, Thread.currentThread(), game.reject_input);
            game.comm_thread =  new Thread(comm);
            game.comm = comm;
            game.comm_thread.start();
        }catch(ClientCommsException e){
            //non si è inizializzato bene
            return false;
        }


        return true;
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
