package client.operations;

import client.ClientJsonUtils;
import client.GameClient;
import client.UserStatus;

public class RequestGameInfoOp extends Operation{

    public RequestGameInfoOp(GameClient game){
        this.game = game;
        this.name = "requestGameInfo";
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
        //chiedo all'utente l'id della partita di cui recuperare le stats
        int id = -1;

        id = get_int("Inserisci l'id del puzzle di cui mostrare" +
                " le statistiche (-1 per la partita corrente): ");

        return ClientJsonUtils.get_requestGameInfo_message(id);
    }

    @Override
    public void on_fail() {}

    @Override
    public void digest(String response) {
        int response_status = ClientJsonUtils.get_int(response, "status", name);
        String desc = ClientJsonUtils.get_description(response, name);
        switch(response_status){
            case 0:
                System.out.println("Ottenute le informazioni sul puzzle\n" + desc);
                break;

            case -1:
                System.out.println("Errore di comunicazione durante il recupero del puzzle");
                break;

            default:
                //comunico all'utente l'errore
                System.out.println("Errore [" + response_status +"]\n\t" + desc);
        }
    }
}
