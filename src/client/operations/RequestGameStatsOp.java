package client.operations;

import client.ClientJsonUtils;
import client.GameClient;
import client.UserStatus;

public class RequestGameStatsOp extends Operation{

    public RequestGameStatsOp(GameClient game){
        this.game = game;
        name = "requestGameStats";
    }

    @Override
    public boolean checks() {
        if(game.u_status != UserStatus.LOGGED_IN){
            System.out.println("---\tDevi aver fatto un accesso prima di poter acccedere ai puzzle");
            return false;
        }

        return true;
    }

    @Override
    public String payload() throws InterruptedException{
        //chiedo all'utente l'id della partita da mostrare

        int id = get_int("Inserisci l'id del puzzle da vedere " +
                "(-1 per la partita corrente): ");

        return ClientJsonUtils.get_requestGameStats_message(id);
    }

    @Override
    public void digest(String response) {
        int response_status = ClientJsonUtils.get_int(response, "status", name);
        String desc = ClientJsonUtils.get_string(response, "description",name);
        switch(response_status){
            case 0:
                System.out.println("Ottenute le statistiche sul puzzle\n" + desc);
                break;

            case -1:
                System.out.println("Errore di comunicazione durante il recupero " +
                        "delle statistiche sul puzzle");
                break;

            default:
                //comunico all'utente l'errore
                System.out.println("Errore [" + response_status +"]\n\t" + desc);
        }
    }

}
