package client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Iterator;
import java.util.List;

//classe che  offre metodi statici per creare i json che il client deve inviare al server
public class ClientJsonUtils {

    //info sulla risposta come descrizioni d'errore
    public static String get_description(String response){
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        return json.get("description").getAsString();
    }

    public static int get_status(String response, String name){
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        if(!json.get("operation").getAsString().equals(name)){
            return -1;
        }

        return json.get("status").getAsInt();
    }

    //rende il messaggio di login date le credenziali
    public static String get_login_message(String user, String password){
        JsonObject json = new JsonObject();
        json.addProperty("operation", "login");
        json.addProperty("username", user);
        json.addProperty("password", password);
        return json.toString();
    }

    //rende il messaggio di logout
    public static String get_logout_message(){
        JsonObject json = new JsonObject();
        json.addProperty("operation", "logout");
        return json.toString();
    }

    //rende il messaggio di register
    public static String get_register_message(String user, String password){
        JsonObject json = new JsonObject();
        json.addProperty("operation", "register");
        json.addProperty("username", user);
        json.addProperty("password", password);
        return json.toString();
    }

    //rende il messaggio di updateCredentials
    public static String get_updateCredentials_message(String user, String password,
                                                        String new_user, String new_password){
        JsonObject json = new JsonObject();
        json.addProperty("operation", "updateCredentials");
        json.addProperty("username", user);
        json.addProperty("password", password);

        if(!new_user.equals("")){
            json.addProperty("newUsername", new_user);
        }

        if(!new_password.equals("")){
            json.addProperty("newPassword", new_password);
        }

        return json.toString();
    }

    //rende il messaggio di submitProposal
    public static String get_submitProposal_message(List<String> words){
        JsonObject json = new JsonObject();
        json.addProperty("operation", "submitProposal");

        JsonArray array = new JsonArray();
        Iterator<String> iter = words.iterator();
        while(iter.hasNext()){
            array.add(iter.next());
        }

        json.add("words", array);

        return json.toString();
    }

    //rende il messaggio di requestGameInfo
    public static String get_requestGameInfo_message(int id){
        JsonObject json = new JsonObject();
        json.addProperty("operation", "requestGameInfo");
        json.addProperty("gameId", id);
        return json.toString();
    }

    //rende il messaggio di requestGameInfo
    public static String get_requestGameStats_message(int id){
        JsonObject json = new JsonObject();
        json.addProperty("operation", "requestGameStats");
        json.addProperty("gameId", id);
        return json.toString();
    }

    public static String get_requestLeaderboard_message(String player_name, int num){
        JsonObject json = new JsonObject();
        json.addProperty("operation", "requestLeaderboard");
        json.addProperty("playerName", player_name);
        json.addProperty("topPlayers", num);
        return json.toString();
    }

}

