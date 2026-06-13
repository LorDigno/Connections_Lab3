package client.operations;

import client.ClientJsonUtils;
import client.GameClient;
import client.UserStatus;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;

public class LogInOp extends Operation {
    //operazione che gestisce la connessione iniziale TCP col server
    private DatagramChannel udp_sock;

    public LogInOp(GameClient game){
        this.game = game;
        this.name = "login";
    }

    @Override
    public boolean checks(){
        if(game.u_status != UserStatus.NOT_LOGGED){
            System.out.println("---\tHai gia eseguito il login come: " + game.username);
            return false;
        }

        boolean sock = connessione();
        if(!sock){
            return false;
        }

        return true;
    }

    @Override
    public String payload() throws InterruptedException{
        //chiedo all'utente le sue credenziali
        String password = "", username = "";

        username = get_string("Inserisci lo username con cui accedere: ");

        password = get_string("Inserisci la password: ");

        //temporaneo, se il login non va a buon fine lo levo in digest
        game.username = username;


        //trovo una porta udp libera
        int udp_port = 0;
        try (DatagramChannel channel = DatagramChannel.open()) {
            //se fai binding su 0 l'os ne assegna una libera
            channel.bind(new InetSocketAddress(0));

            //ricavo la porta per inviarla al server
            InetSocketAddress localAddress = (InetSocketAddress) channel.getLocalAddress();
            udp_port = localAddress.getPort();

            //lo chiudo se il login non ha successo
            this.udp_sock = channel;
        } catch (IOException e) {
            System.err.println("Errore I/O durante l'apertura o il bind del DatagramChannel: " + e.getMessage());
        }

        //il payload da inviare
        return ClientJsonUtils.get_login_message(username, password, udp_port);
    }

    @Override
    public void on_fail(){
        game.reset();
    }

    @Override
    public void digest(String response){
        String username = game.username;

        int response_status = ClientJsonUtils.get_int(response, "status", name);
        String desc = ClientJsonUtils.get_string(response,"descriprition",name);

        if (response_status == 0) {
            //confermo la riuscita del login e cambio status
            //tcp_sock e username sono già associati al GameClient
            System.out.println("Accesso eseguito con successo, benvenuto " + username + " !!\n" +
                    "E' ora di iniziare una partita!!!\n\n" + desc);

            //aggiungo l'id della partita
            int new_id = ClientJsonUtils.get_int(response, "new_id", name);
            game.puzzle_id.set(new_id);

            //aggiungo il channel di ricezione delle modifiche
            game.comm.add_udp_channel(udp_sock, game.puzzle_id);

            //fatto tutto
            game.u_status = UserStatus.LOGGED_IN;
            return;
        }

        //resetto lo status del GameClient
        game.reset();

        try{
            udp_sock.close();
        }catch (IOException e){
            System.err.println("### Errore nella chiusura del canale udp");
        }

        if(response_status == -1) {
            System.out.println("Errore di comunicazione durante l'accesso");
            return;
        }

        //comunico all'utente l'errore
        System.out.println("Errore [" + response_status +"]\n\t" + desc);
    }
}
