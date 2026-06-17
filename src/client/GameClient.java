package client;
import client.operations.*;

import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


public class GameClient {
    //classe che implementa il gioco a linea di comando
    public int port, timeout;
    public String server_host, username;
    public UserStatus u_status;
    public List<String> banlist;
    public AtomicBoolean reject_input;
    public AtomicInteger puzzle_id;
    private BlockingQueue<String> input_queue;

    //usati per gestire la chiusura della comunicazione
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
        this.puzzle_id = new AtomicInteger(-1);
    }

    ///Main lifecycle of the GameClient
    public void launch(){
        while(true) {
            try{
                if(Thread.currentThread().isInterrupted()){
                    throw new InterruptedException();
                }

                System.out.print("> ");

                //prende e interpreta l'input dell'utente
                Operation op = op_choice(get_input());
                if(op != null){
                    //esegue l'azione
                    op.execute();
                }
            }catch(InterruptedException e){
                input_queue.clear();
                reject_input.set(false);
                if(comm.allow_op.get() == 0){
                    reset();
                }
                else{
                    comm.allow_op.set(0);
                }

            }
        }
    }

    ///Capisce che operazione fare dato l'input con uno switch della morte
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
                break;
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

    ///Chiude il socketChannel, e il thread di comunicazione resetta lo stato del gameClient
    public void reset() {
        //ciudo la connessione tcp
        try {
            if(comm != null){
                comm.selector.close();
            }
            if(comm_thread != null){
                comm_thread.join();
            }
        }catch(InterruptedException e){
            //teoricamente dopo l'annullamento di tutti i riferimenti subentra il garbage collector
        } catch (IOException e) {
            comm_thread.interrupt();
        }

        //reset del GameClient
        u_status = UserStatus.NOT_LOGGED;
        username = null;
        comm = null;
        comm_thread = null;
        puzzle_id.set(-1);

        System.out.println("---Stato del client azzerato");
    }

    ///Esegue il logout e chiude tutto
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

        reset();

        System.out.println("\nArrivederci e grazie per aver giocato!!\n");
        System.exit(0);
    }

    ///Stampa a schermo le informazioni sui comandi possibili
    private void help(){
        System.out.println("Le operazioni possibili sono:  (tutte non case sensitive)\n" +
                "\n- Operazioni di gestione del profilo\n" +
                "\t*** \"login\", fa immettere le credenziali ed accedere al gioco col proprio profilo. (solo da disconnesso)\n" +
                "\t*** \"logout\", interrompe la connessione col server ed esce dal profilo. (solo da connesso)\n" +
                "\t*** \"register\", permette di creare un profilo nuovo dati un username (libero) e la password\n" +
                "\t*** \"updateCredentials\", cambia le credenziali di un account di cui le hai quelle attuali\n" +
                "\n- Operazioni di gestione del puzzle (solo da connesso)\n" +
                "\t*** \"submitProposal\", fa inserire 4 parole per indovinare un gruppo\n" +
                "\t*** \"requestGameInfo\", mostra le informazioni di gioco sul puzzle attivo o uno vecchio\n" +
                "\t*** \"requestGameStats\", mostra le statistiche dei giocatori sul puzzle specificato o l'attivo\n" +
                "\n- Operazioni di gestione del puzzle (solo da connesso)\n" +
                "\t*** \"requestLeaderboard\", mostra i punteggi di tutti/dei k migliori/di un giocatore specifico\n" +
                "\t*** \"requestPlayerStats\", mostra varie statistiche sul tuo profilo\n" +
                "\n- Operazioni di sistema\n" +
                "\t*** \"quit\", esci dal gioco.\n" +
                "\t*** \"help\", mostra le azioni possibili (ma credo che lo sapessi già)\n");
    }
}
