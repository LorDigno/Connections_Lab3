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
        //prime info sul gioco
        System.out.println("Benvenuto in connections");

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
            if(op == null){
                System.out.println("L'operazione \"" + input + "\" non esiste.");
                continue;
            }

            //esegue l'azione
            op.execute();
        }
    }

    //capisce che operazione fare dato l'input
    public Operation op_choice(String input){
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
            case "requestleaderboard":
                op = new RequestLeaderboardOp(this);
                break;
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
}
