package client.operations;

import client.ClientJsonUtils;
import client.GameClient;
import client.UdpHandler;
import client.UserStatus;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;

public class LogInOp extends Operation {
    //operazione che gestisce la connessione iniziale TCP col server

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

        SocketChannel sock = connessione();
        if(sock == null){
            return false;
        }

        //da usare in communicate, se non va a buon fine lo tolgo in digest
        game.tcp_sock = sock;
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

        //il payload da inviare
        return ClientJsonUtils.get_login_message(username, password);
    }

    @Override
    public void on_fail(){
        game.reset();
    }

    @Override
    public void digest(String response){
        SocketChannel sock = game.tcp_sock;
        String username = game.username;

        int response_status = ClientJsonUtils.get_int(response, "status", name);
        String desc = ClientJsonUtils.get_description(response);

        if (response_status == 0) {
            //confermo la riuscita del login e cambio status
            //tcp_sock e username sono già associati al GameClient
            System.out.println("Accesso eseguito con successo, benvenuto " + username + " !!\n" +
                    "E' ora di iniziare una partita!!!\n\n" + desc);

            //inizializzazione del thread in ascolto per udp
            int udp_port = ClientJsonUtils.get_int(response, "udpPort", "login");

            DatagramChannel udp_sock = null;
            while(udp_sock == null) {
                try {
                    InetSocketAddress ad = new InetSocketAddress(udp_port);
                    udp_sock = DatagramChannel.open().bind(ad);
                } catch (IOException e) {
                    //retry
                    udp_sock = null;
                }
            }

            game.udp_sock = udp_sock;
            Thread udp = new Thread(new UdpHandler(udp_sock, game.reject_input, Thread.currentThread()));
            udp.start();



            //fatto tutto
            game.u_status = UserStatus.LOGGED_IN;
            return;
        }

        //chiudo la connessione tcp
        try {
            sock.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //resetto lo status del GameClient
        game.reset();

        if(response_status == -1) {
            System.out.println("Errore di comunicazione durante l'accesso");
            return;
        }

        //comunico all'utente l'errore
        System.out.println("Errore [" + response_status +"]\n\t" + desc);

    }
}
