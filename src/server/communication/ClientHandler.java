package server.communication;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import server.game.GameManager;
import server.users.UserManager;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

///Task runnable che gestisce la comunicazione col client e chiama i metodi delle altre componenti
public class ClientHandler implements Runnable{
    private Socket sock;
    private boolean done;

    //managers
    private GameManager game_m;
    private UserManager user_m;

    //per l'utente
    private int user_id;

    public ClientHandler(Socket tcp_sock, GameManager game_m, UserManager user_m){
        sock = tcp_sock;
        this.game_m = game_m;
        this.user_m = user_m;
        done = false;

        //inizializzato al login
        user_id = -1;
    }

    @Override
    public void run() {
        try(Socket s = sock; //così lo chiude in atoumatico alla fine
            DataInputStream in = new DataInputStream(new BufferedInputStream(sock.getInputStream()));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(sock.getOutputStream()))){

            while(true){
                //aspetto l'arrivo della richiesta
                int dim = in.readInt();

                //preparo la ricezione del payload in json
                ByteBuffer buf = ByteBuffer.allocate(dim);
                in.readFully(buf.array());

                buf.flip();
                String request = new String(buf.array(), StandardCharsets.UTF_8);

                //eseguo l'operazione richiesta e prendo il payload
                String response = handle_request(request);

                //invio la risposta
                if(response != null){
                    byte[] res = response.getBytes(StandardCharsets.UTF_8);
                    buf = ByteBuffer.allocate(4 + res.length);
                    buf.putInt(res.length);
                    buf.put(res);
                    out.write(buf.array());
                    out.flush();
                }
                System.out.println(response);

                if(done){
                    break;
                }
            }

        //un'eccezione fa terminare il thread
        }catch(IOException e){
            System.err.println("Client disconnesso o errore di I/O: " + e.getMessage());
        }catch(Exception e) {
            System.err.println("ERRORE CRITICO NEL SERVER DURANTE L'ELABORAZIONE:");
            e.printStackTrace();
        }

        clear();
    }

    ///Gestisce la sincronizazzione con endgame via start_action ed end_action.
    ///Usa uno switch della morte per capire quale metodo chiamare.
    private String handle_request(String request){
        String payload = null;

        JsonObject json = JsonParser.parseString(request).getAsJsonObject();
        String operation = json.get("operation").getAsString();

        //avvio della richiesta
        if (!game_m.try_start_action()) {
            StatusDescription sd = new StatusDescription();
            sd.setStatus(ResponseStatus.GAME_CHANGING);
            sd.setDescription("Il server non accetta richieste durante la generazione di un nuovo puzzle");
            return build_response(operation, sd);
        }

        try {
            switch(operation){
                //casistiche per ogni operation possibile, creano i payload in json
                case "login":
                    payload = login_method(json);
                    break;

                case "logout":
                    payload = logout_method();
                    break;

                case "register":
                    payload = register_method(json);
                    break;

                case "updateCredentials":
                    payload = update_method(json);
                    break;

                case "submitProposal":
                    payload = proposal_method(json);
                    break;

                case "requestGameInfo":
                    payload = puzzle_info_method(json);
                    break;

                case "requestGameStats":
                    payload = puzzle_stats_method(json);
                    break;

                case "requestPlayerStats":
                    payload = player_stats_method();
                    break;

                case "requestLeaderboard":
                    payload = leaderboard_method(json);
                    break;

                default:
                    //operazione non nota
                    done = true;
                    return null;
            }
        }finally{
            //operazione a mezzo finita
            game_m.end_action();
        }

        return payload;
    }

    ///Crea la struttura del json della risposta dati i payload
    private String build_response(String op_name, StatusDescription sd){
        JsonObject json = new JsonObject();
        json.addProperty("operation", op_name);
        json.addProperty("status", sd.getStatus().getCode());
        json.addProperty("description", sd.getDescription());
        return json.toString();
    }

    private String login_method(JsonObject json){
        String username = json.get("username").getAsString();
        String password = json.get("psw").getAsString();
        int udp_port = json.get("udp_port").getAsInt();

        StatusDescription sd = user_m.session_login(username, password,
                sock.getInetAddress(), udp_port);

        if(sd.getStatus() == ResponseStatus.OK){
            user_id = user_m.get_id_from_username(username);
            game_m.log_participant(user_id);
        }

        //creo il payload a mano perché c'è anche new_id
        JsonObject new_json = new JsonObject();
        new_json.addProperty("operation", "login");
        new_json.addProperty("status", sd.getStatus().getCode());
        new_json.addProperty("description", sd.getDescription());
        new_json.addProperty("new_id", game_m.current_id());
        return new_json.toString();
    }

    //non richiede input dato che ha solo il campo operation
    private String logout_method(){
        StatusDescription sd = user_m.logout(user_id);
        return build_response("logout", sd);
    }

    private String register_method(JsonObject json){
        String username = json.get("username").getAsString();
        String password = json.get("psw").getAsString();

        StatusDescription sd = user_m.register(username, password);

        return build_response("register", sd);
    }

    private String update_method(JsonObject json){
        String username = json.get("oldUsername").getAsString();
        String password = json.get("oldPsw").getAsString();

        String new_u = null, new_p = null;
        if(json.get("newUsername") != null){
            new_u = json.get("newUsername").getAsString();
        }
        if(json.get("newPsw") != null){
            new_p = json.get("newPsw").getAsString();
        }

        StatusDescription sd = user_m.update_credentials(username, password, new_u, new_p);

        return build_response("updateCredentials", sd);
    }

    private String proposal_method(JsonObject json){
        List<String> words = new ArrayList<>();
        int id = json.get("puzzle_id").getAsInt();

        //estraggo le stringhe dal json
        JsonArray jsonArray = json.get("words").getAsJsonArray();
        for (int i = 0; i < jsonArray.size(); i++) {
            words.add(jsonArray.get(i).getAsString());
        }

        StatusDescription sd = game_m.submit_proposal(user_id, id, words);
        return build_response("submitProposal", sd);
    }

    private String puzzle_info_method(JsonObject json){
        int id = json.get("gameId").getAsInt();

        StatusDescription sd = game_m.get_puzzle_info(user_id, id);
        return build_response("requestGameInfo", sd);
    }

    private String puzzle_stats_method(JsonObject json){
        int id = json.get("gameId").getAsInt();

        StatusDescription sd = game_m.get_puzzle_stats(id);
        return build_response("requestGameStats", sd);
    }

    //niente json che ha solo il campo operation
    private String player_stats_method(){
        StatusDescription sd = user_m.get_player_stats(user_id);
        return build_response("requestPlayerStats", sd);
    }

    private String leaderboard_method(JsonObject json){
        String who = json.get("playerName").getAsString();
        int num = json.get("topPlayers").getAsInt();

        StatusDescription sd = user_m.get_overall_leaderboard(who, num, user_id);
        return build_response("requestLeaderboard", sd);
    }

    ///Se c'è roba da liberare lo faccio qua
    private void clear(){
        if(user_id != -1){
            user_m.remove_active(user_id);
        }
    }
}
