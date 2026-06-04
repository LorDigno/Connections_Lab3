package client.operations;

import client.ClientJsonUtils;
import client.GameClient;
import client.UserStatus;

import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class UpdateCredentialsOp extends Operation {

    private boolean clear = false;
    public UpdateCredentialsOp(GameClient game){
        this.game = game;
        this.name = "updateCredentials";
    }

    @Override
    public boolean checks(){
        //se non sono loggato non ho una connessione tcp aperta
        if(game.u_status == UserStatus.NOT_LOGGED || game.sock == null){
            clear = true;

            //creo un socketChannel temporaneo da richiudere in digest e on_fail
            SocketChannel sock = connessione();
            if(sock == null){
                return false;
            }

            game.sock = sock;
        }
        return true;
    }

    @Override
    public String payload() throws InterruptedException{
        //chiedo all'utente i dati dell'account che vuole cambiare
        String password = "", username = "";

        username = get_string("Inserisci lo username del profilo da modificare: ");

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
    public void on_fail(){
        if(clear){
            game.reset();
        }
    }

    @Override
    public void digest(String response) {
        int response_status = ClientJsonUtils.get_int(response, "status", name);
        String desc = ClientJsonUtils.get_description(response);
        switch(response_status){
            case 0:
                System.out.println("Cambio delle credenziali completato con successo");
                break;

            case -1:
                System.out.println("Errore di comunicazione durante l'aggiornamento delle credenziali");
                break;

            default:
                //comunico all'utente l'errore
                System.out.println("Errore [" + response_status +"]\n\t" + desc);
        }

        if(clear) {
            game.reset();
        }
    }
}
