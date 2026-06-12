package server.communication;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import server.game.GameManager;
import server.puzzles.UserPuzzle;
import server.users.UserManager;
import server.users.UserSession;

import javax.net.ssl.SSLEngineResult;
import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

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

                if(done){
                    //se ci sono eccezioni o alcune op come logout
                    break;
                }
            }

        }catch(IOException e){
            //tbd
        }

    }

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
                    done = true;
                    break;

                case "register":
                    payload = register_method(json);
                    done = true;
                    break;

                default:
                    //operazione non nota
                    return null;
            }
        }finally{
            //operazione a mezzo finita
            game_m.end_action();
        }

        return payload;
    }

    //crea la struttura del json della risposta dati i payload
    private String build_response(String op_name, StatusDescription sd){
        JsonObject json = new JsonObject();
        json.addProperty("operation", op_name);
        json.addProperty("status", sd.getStatus().getCode());
        json.addProperty("description", sd.getDescription());
        return json.toString();
    }

    private String login_method(JsonObject json){
        String username = json.get("username").getAsString();
        String password = json.get("password").getAsString();
        int udp_port = json.get("udp_port").getAsInt();

        StatusDescription sd = user_m.session_login(username, password,
                sock.getInetAddress(), udp_port);

        if(sd.getStatus() == ResponseStatus.OK){
            user_id = user_m.get_id_from_username(username);
            game_m.log_participant(user_id);
        }else{
            done = true;
        }

        return build_response("login", sd);
    }

    //non richiede input dato che ha solo il campo operation
    private String logout_method(){
        StatusDescription sd = user_m.logout(user_id);
        return build_response("logout", sd);
    }

    private String register_method(JsonObject json){
        String username = json.get("username").getAsString();
        String password = json.get("password").getAsString();

        StatusDescription sd = user_m.register(username, password);

        return build_response("register", sd);
    }
}
