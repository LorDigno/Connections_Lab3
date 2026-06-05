package client;
import client.operations.*;

import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;


public class GameClient {
    //classe che implementa il gioco a linea di comando
    public int port, timeout;
    public String server_host, username;
    public UserStatus u_status;
    public SocketChannel tcp_sock = null;
    public List<String> banlist;
    public AtomicBoolean reject_input;
    private BlockingQueue<String> input_queue;
    public DatagramChannel udp_sock;
    public Thread comm_thread;
    public Communication comm;

    public GameClient(String host, int port, int timeout, List<String> banlist, BlockingQueue<String> input_queue, AtomicBoolean reject_input){
        server_host= host;
        this.port = port;
        this.timeout = timeout;
        u_status = UserStatus.NOT_LOGGED;
        this.banlist = banlist;
        this.input_queue = input_queue;
        this.reject_input = reject_input;

    }

    //main lifecycle of the GameClient
    public void launch(){
        while(true) {
            try{
                if(Thread.currentThread().isInterrupted()){
                    throw new InterruptedException();
                }

                //get the player's input
                System.out.print("> ");

                //reacts to the players input
                Operation op = op_choice(get_input());
                if(op != null){
                    //esegue l'azione
                    op.execute();
                }
            }catch(InterruptedException e){
                input_queue.clear();
                reject_input.set(false);
            }
        }
    }

    //capisce che operazione fare dato l'input
    private Operation op_choice(String input){
        Operation op = null;
        switch(input){
            case "login":
                op = new LogInOp(this);
                break;
            case "logout":
                op = new LogOutOp(this);
                break;
            case "register":
                op = new RegisterOp(this, banlist);
                break;
            case "updatecredentials":
                op = new UpdateCredentialsOp(this);
                break;
            case "submitproposal":
                op = new SubmitProposalOp(this);
                break;
            case "requestgameinfo":
                op = new RequestGameInfoOp(this);
                break;
            case "requestgamestats":
                op = new RequestGameStatsOp(this);
                break;
            case "requestleaderboard":
                op = new RequestLeaderboardOp(this);
                break;
            case "requestplayerstats":
                op = new RequestPlayerStatsOp(this);
                break;
            case "quit":
                quit();
            case "help":
                help();
                break;
            default:
                System.out.println("L'operazione \"" + input + "\" non esiste.");
        }
        return op;
    }

    public String get_input() throws InterruptedException{
        return input_queue.take();
    }

    //chiude il socketChannel e resetta lo stato del gameClient
    public void reset() {
        //ciudo la connessione tcp
        try {
            if(tcp_sock != null){
                tcp_sock.close();
            }
            if(udp_sock != null){
                udp_sock.close();
                udp_thread.join();
            }
        }catch (InterruptedException e){
            //teoricamente dopo l'annullamento di tutti i riferimenti subentra il garbage collector
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //reset del GameClient
        u_status = UserStatus.NOT_LOGGED;
        tcp_sock = null;
        username = null;
        udp_sock = null;
        udp_thread = null;
    }

    private void quit(){
        try{
            if(this.u_status == UserStatus.LOGGED_IN){
                System.out.println("Disconnessione automatica per la terminazione del gioco");
                new LogOutOp(this).execute();
            }
        }
        catch(InterruptedException e){
            System.err.println("Interruzione in quit");
        }

        System.out.println("\nArrivederci e grazie per aver giocato!!\n");
        System.exit(0);
    }

    private void help(){
        System.out.println("Le operazioni possibili sono:  (tutte non case sensitive)\n" +
                "\n- Operazioni di gestione del profilo\n" +
                "\t*** \"login\", fa immettere le credenziali ed accedere al gioco col proprio profilo. (solo da disconnesso)\n" +
                "\t*** \"logout\", interrompe la connessione col server ed esce dal profilo. (solo da connesso)\n" +
                "\t*** \"register\", permette di creare un profilo nuovo dati un username (libero) e la password\n" +
                "\t*** \"updateCredentials\", cambia le credenziali di un account di cui le hai quelle attuali\n" +
                "\n- Operazioni di gestione del puzzle (solo da connesso)\n" +
                "\t*** \"submitProposal\", fa inserire 4 parole per indovinare un gruppo\n" +
                "\t*** \"requestGameStatus\", mostra le informazioni di gioco sul puzzle attivo o uno vecchio\n" +
                "\t*** \"requestGameStats\", mostra le statistiche dei giocatori sul puzzle specificato o l'attivo\n" +
                "\n- Operazioni di gestione del puzzle (solo da connesso)\n" +
                "\t*** \"requestLeaderboard\", mostra i punteggi di tutti/dei k migliori/di un giocatore specifico\n" +
                "\t*** \"requestPlayerStats\", mostra varie statistiche sul tuo profilo\n" +
                "\n- Operazioni di sistema\n" +
                "\t*** \"quit\", esci dal gioco.\n" +
                "\t*** \"help\", mostra le azioni possibili (ma credo che lo sapessi già)\n");
    }
}
