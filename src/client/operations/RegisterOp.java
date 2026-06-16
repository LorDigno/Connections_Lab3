package client.operations;

import client.ClientJsonUtils;
import client.GameClient;
import client.UserStatus;

import java.util.List;
import java.util.Scanner;

public class RegisterOp extends Operation {
    private String user;
    private List<String> banned_users;

    public RegisterOp(GameClient game, List<String> banlist){
        this.game = game;
        this.name = "register";
        banned_users = banlist;
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
        //chiedo all'utente i dati con cui si vuole registare
        String password = "", username = "";

        Scanner scanner = new Scanner(System.in);
        while(true){
            username = get_string("Inserisci lo username con cui ti vuoi registrare: ");

            if(!banned_users.contains(username)){
                break;
            }

            System.out.println("--- Lo username: +\""+username+"\" non è valido");
        }

        password = get_string("Inserisci la password: ");

        this.user = username;

        //il payload da inviare
        return ClientJsonUtils.get_register_message(username, password);
    }

    @Override
    public void digest(String response) {
        int response_status = ClientJsonUtils.get_int(response, "status", name);
        switch (response_status) {
            case 0:
                System.out.println("Registrazione dell'utente: \"" + user + "\" completata con " +
                        "successo.\nPuoi accedere utilizzando login");
                break;

            case -1:
                System.out.println("Errore di comunicazione durante la disconnessione");
                game.reset();
                break;

            default:
                //comunico all'utente l'errore
                String desc = ClientJsonUtils.get_string(response, "description",name);
                System.out.println("Errore [" + response_status +"]\n\t" + desc);
                game.reset();
        }
    }
}
