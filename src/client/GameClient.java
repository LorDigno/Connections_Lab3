package client;

import client.operations.*;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Scanner;


public class GameClient {
    //classe che implementa il gioco a linea di comando
    public int port, timeout;
    public String server_host, username;
    public UserStatus u_status;
    public SocketChannel sock = null;
    public List<String> banlist;

    public GameClient(String host, int port, int timeout, List<String> banlist){
        server_host= host;
        this.port = port;
        this.timeout = timeout;
        u_status = UserStatus.NOT_LOGGED;
        this.banlist = banlist;
    }

    //main lifecycle of the GameClient
    public void launch(){

        while(true){
            //get the player's input
            String input = "";
            Scanner scanner = new Scanner(System.in);
            System.out.print("> ");

            if (scanner.hasNextLine()) {
                input = scanner.nextLine().strip().toLowerCase();
            }

            //reacts to the players input
            Operation op = op_choice(input);
            if(op != null){
                //esegue l'azione
                op.execute();
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

    //chiude il socketChannel e resetta lo stato del gameClient
    public void reset() {
        //ciudo la connessione tcp
        try {
            sock.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //reset del GameClient
        u_status = UserStatus.NOT_LOGGED;
        sock = null;
        username = null;
    }

    private void quit(){
        if(this.u_status == UserStatus.LOGGED_IN){
            System.out.println("Disconnessione automatica per la terminazione del gioco");
            new LogOutOp(this).execute();
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
