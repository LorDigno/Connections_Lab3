package client.operations;

import client.ClientJsonUtils;
import client.GameClient;
import client.UserStatus;

public class LogOutOp extends Operation { ;
    public LogOutOp(GameClient game){
        this.game = game;
        this.name = "logout";
    }

    @Override
    public boolean checks(){
        if(game.u_status != UserStatus.LOGGED_IN){
            System.out.println("---\tDevi aver fatto un accesso prima di poterti disconnettere");
            return false;
        }
        if(game.comm_thread == null){
            System.err.println("### Invio senza connessione prestabilita in logout");

            //sistemo lo stato del GameClient
            game.u_status = UserStatus.NOT_LOGGED;
            game.username = null;
            return false;
        }

        return true;
    }

    @Override
    public String payload(){
        return ClientJsonUtils.get_logout_message();
    }

    @Override
    public void on_fail(){
        game.reset();
    }

    @Override
    public void digest(String response) {
        int response_status = ClientJsonUtils.get_int(response, "status", name);

        switch (response_status) {
            case 0:
                System.out.println("Disconnessione eseguita con successo, arrivederci " + game.username + " !!");
                break;

            case -1:
                System.out.println("Errore di comunicazione durante la disconnessione");
                break;

            default:
                //comunico all'utente l'errore
                String desc = ClientJsonUtils.get_description(response, name);
                System.out.println("Errore [" + response_status +"]\n\t" + desc);
        }

        //da fare a prescindere dalla buona riuscita della comunicazione
        game.reset();
    }
}

