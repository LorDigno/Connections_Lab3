package client.operations;

import client.ClientJsonUtils;
import client.GameClient;
import client.UserStatus;

public class RequestLeaderboardOp extends Operation {

    public RequestLeaderboardOp(GameClient game){
        this.game = game;
        name = "requestLeaderboard";
    }

    @Override
    public boolean checks() {
        if(game.u_status != UserStatus.LOGGED_IN){
            System.out.println("---\tDevi aver fatto un accesso prima di poter vedere la classifica");
            return false;
        }

        return true;
    }

    @Override
    public String payload() throws InterruptedException{
        String in = "", user = "none";
        int num = 1;

        while(true){
            System.out.print("Vuoi vedere un utente specifico? (y/n):");
            in = game.get_input();

            if(in.equals("y")){
               user = get_string("Inserisci lo username dell'utente: ");
               break;
            }
            if(in.equals("n")){
                num = get_int("Il numero di utenti da mostrare (0 per tutti): ");
                break;
            }

            System.out.print("---Opzione \"" + in + "\" non valida\n");
        }

        return ClientJsonUtils.get_requestLeaderboard_message(user, num);
    }

    @Override
    public void on_fail() {}

    @Override
    public void digest(String response) {
        int response_status = ClientJsonUtils.get_int(response, "status", name);
        String desc = ClientJsonUtils.get_description(response, name);
        switch(response_status){
            case 0:
                System.out.println(desc);
                break;

            case -1:
                System.out.println("Errore di comunicazione durante il recupero " +
                        "della classifica");
                break;

            default:
                //comunico all'utente l'errore
                System.out.println("Errore [" + response_status +"]\n\t" + desc);
        }
    }
}
