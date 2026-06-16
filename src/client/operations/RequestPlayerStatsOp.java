package client.operations;

import client.ClientJsonUtils;
import client.GameClient;
import client.UserStatus;

public class RequestPlayerStatsOp extends Operation{

    public RequestPlayerStatsOp(GameClient game){
        this.game = game;
        name = "requestPlayerStats";
    }

    @Override
    public boolean checks() {
        if(game.u_status != UserStatus.LOGGED_IN){
            System.out.println("---\tDevi aver fatto un accesso prima di poter vedere le tue informazioni");
            return false;
        }

        return true;
    }

    @Override
    public String payload() {
        return ClientJsonUtils.get_requestPlayerStats_message();
    }

    @Override
    public void digest(String response) {
        int response_status = ClientJsonUtils.get_int(response, "status", name);
        String desc = ClientJsonUtils.get_string(response, "description",name);
        switch(response_status){
            case 0:
                System.out.println("Ecco le tue statistiche personali \"" + game.username + "\"\n"
                        + desc);
                break;

            case -1:
                System.out.println("Errore di comunicazione durante il recupero delle statistiche" +
                        "personali");
                break;

            default:
                //comunico all'utente l'errore
                System.out.println("Errore [" + response_status +"]\n\t" + desc);
        }
    }
}
