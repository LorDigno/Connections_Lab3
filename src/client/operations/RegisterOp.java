package client.operations;

import client.ClientJsonUtils;
import client.GameClient;
import client.UserStatus;

import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class RegisterOp extends Operation {
    private String user;
    private boolean clear = false;

    public RegisterOp(GameClient game){
        this.game = game;
        this.name = "register";
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
    public String payload() {
        //chiedo all'utente i dati con cui si vuole registare
        String password = "", username = "";

        Scanner scanner = new Scanner(System.in);
        System.out.print("Inserisci lo username con cui ti vuoi registrare: ");
        if (scanner.hasNextLine()) {
            username = scanner.nextLine().strip();
        }
        System.out.print("Inserisci la password: ");
        if (scanner.hasNextLine()) {
            password = scanner.nextLine().strip();
        }

        this.user = username;

        //il payload da inviare
        return ClientJsonUtils.get_register_message(username, password);
    }

    @Override
    public void on_fail(){
        if(clear){
            game.reset();
        }
    }

    @Override
    public void digest(String response) {
        int response_status = ClientJsonUtils.get_status(response, name);
        switch (response_status) {
            case 0:
                System.out.println("Registrazione dell'utente: \"" + user + "\" completata con " +
                        "successo.\nPuoi accedere utilizzando login");
                break;

            case -1:
                System.out.println("Errore di comunicazione durante la disconnessione");
                break;

            default:
                //comunico all'utente l'errore
                String desc = ClientJsonUtils.get_description(response);
                System.out.println("Errore [" + response_status +"]\n\t" + desc);
        }

        if(clear) {
            game.reset();
        }
    }
}
