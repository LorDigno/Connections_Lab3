package client.operations;

import client.ClientJsonUtils;
import client.GameClient;
import client.UserStatus;

public class UpdateCredentialsOp extends Operation {

    private String old;
    public UpdateCredentialsOp(GameClient game){
        this.game = game;
        this.name = "updateCredentials";
    }

    @Override
    public boolean checks(){
        //se non sono loggato non ho una connessione tcp aperta
        if(game.u_status == UserStatus.NOT_LOGGED || game.comm_thread == null){
            //creo un socketChannel temporaneo da richiudere in digest e on_fail
            boolean sock = connessione();
            if(!sock){
                return false;
            }
        }
        return true;
    }

    @Override
    public String payload() throws InterruptedException{
        //chiedo all'utente i dati dell'account che vuole cambiare
        String password = "", username = "";

        username = get_string("Inserisci lo username del profilo da modificare: ");
        old = username;

        password =  get_string("Inserisci la password: ");

        //chiedo all'utente i dati nuovi
        String new_password = "", new_username = "";
        while(true){
            System.out.print("Inserisci lo username nuovo (o solo invio per lasciarlo invariato): ");
            new_username = game.get_input();
            System.out.print("Inserisci la password nuova (o solo invio per lasciarlo invariata): ");
            new_password = game.get_input();

            if(!new_password.equals("") || !new_username.equals("")){
                break;
            }
            System.out.println("Devi modificare almeno uno fra username e password e non" +
                    "ti lascio andare finché non lo fai!!");
        }

        return ClientJsonUtils.get_updateCredentials_message(username, password, new_username, new_password);
    }

    @Override
    public void digest(String response) {
        int response_status = ClientJsonUtils.get_int(response, "status", name);
        String desc = ClientJsonUtils.get_string(response, "description",name);
        switch(response_status){
            case 0:
                System.out.println("Cambio delle credenziali completato con successo");

                String new_username = ClientJsonUtils.get_string(response, "newUsername", name);
                if(new_username != null && game.username == old){
                    game.username = new_username;
                }

                break;

            case -1:
                System.out.println("Errore di comunicazione durante l'aggiornamento delle credenziali");
                game.reset();
                break;

            default:
                //comunico all'utente l'errore
                System.out.println("Errore [" + response_status +"]\n\t" + desc);
                game.reset();
        }
    }
}
