package server.communication;

import com.google.gson.JsonObject;
import server.puzzles.RealPuzzle;
import server.users.UserSession;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public class UDPNotifier {

    public UDPNotifier(){}

    public void kick(UserSession session){
        //controllo per sicurezza
        if (session.address == null || session.udp_port <= 0) {
            return;
        }

        JsonObject json = new JsonObject();
        json.addProperty("operation", "udpKick");

        String description = "Sei stato disconnesso perché sono state cambiate le credenziali del profilo";
        json.addProperty("description", description);

        byte[] buffer = json.toString().getBytes(StandardCharsets.UTF_8);

        try(DatagramSocket socket = new DatagramSocket()){
            //creo e invio il datagramma
            DatagramPacket packet = new DatagramPacket(
                    buffer,
                    buffer.length,
                    session.address,
                    session.udp_port
            );
            socket.send(packet);
        }catch(IOException e){
            //non si apre il datagramSocket o è fallito l'invio
            System.err.println("Non si apre il DatagramSocket o è fallito l'invio in kick");
        }
    }

    public void notify_active(RealPuzzle old_game, RealPuzzle current, String leaderboard,
                              Set<UserSession> logged){
        //se non ci sono utenti loggati
        if (logged == null || logged.isEmpty()){
            return;
        }

        JsonObject json = new JsonObject();
        json.addProperty("operation", "udpPuzzleTermination");
        json.addProperty("new_id", current.id);

        String description = "Partita numero: " + old_game.id + " terminata, ecco la classifica dei risultati:\n\n" +
                leaderboard +
                "\n\nPuoi vedere la nuova partita con requestGameInfo della partita corrente\n";
        json.addProperty("description", description);

        byte[] buffer = json.toString().getBytes(StandardCharsets.UTF_8);

        //ciclo d'invio a tutti gli utenti attivi
        try(DatagramSocket socket = new DatagramSocket()){
            for (UserSession session : logged) {
                //controllo per sicurezza
                if (session.address == null || session.udp_port <= 0) {
                    continue;
                }

                try{
                    //creo e invio il datagramma
                    DatagramPacket packet = new DatagramPacket(
                            buffer,
                            buffer.length,
                            session.address,
                            session.udp_port
                    );
                    socket.send(packet);
                }catch(IOException e){
                    //ad un utente è fallito
                    System.err.println("Fallito l'invio della notifica a " + session.username);
                }
            }
        }catch(IOException e){
            System.err.println("Non si apre il DatagramSocket della notifica");
        }
    }
}
