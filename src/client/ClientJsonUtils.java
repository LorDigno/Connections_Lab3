package client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Iterator;
import java.util.List;

///Classe che offre metodi statici per creare i json che il client deve inviare al server
public class ClientJsonUtils {

    ///Estrae da response la proprietà property(String), se non c'è o il campo operations è diverso da
    /// quello dato rende null;
    public static String get_string(String response, String property,String op_name){
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        if(!json.get("operation").getAsString().equals(op_name)){
            return null;
        }

        if(json.get(property) != null){
            return json.get(property).getAsString();
        }

        return null;
    }

    ///Estrae da response la proprietà property(int), se non c'è o il campo operations è diverso da
    /// quello dato rende null;
    public static int get_int(String response, String property, String op_name){
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        if(!json.get("operation").getAsString().equals(op_name)){
            return -1;
        }

        return json.get(property).getAsInt();
    }

    ///Rende il messaggio di login date le credenziali e la porta udp
    public static String get_login_message(String user, String password, int port){
        JsonObject json = new JsonObject();
        json.addProperty("operation", "login");
        json.addProperty("username", user);
        json.addProperty("psw", password);
        json.addProperty("udp_port", port);
        return json.toString();
    }

    ///Rende il messaggio di logout
    public static String get_logout_message(){
        JsonObject json = new JsonObject();
        json.addProperty("operation", "logout");
        return json.toString();
    }

    ///Rende il messaggio di register
    public static String get_register_message(String user, String password){
        JsonObject json = new JsonObject();
        json.addProperty("operation", "register");
        json.addProperty("username", user);
        json.addProperty("psw", password);
        return json.toString();
    }

    ///Rende il messaggio di updateCredentials, i campi new vengono aggiunti solo se diversi da ""
    public static String get_updateCredentials_message(String user, String password,
                                                        String new_user, String new_password){
        JsonObject json = new JsonObject();
        json.addProperty("operation", "updateCredentials");
        json.addProperty("oldUsername", user);
        json.addProperty("oldPsw", password);

        if(!new_user.equals("")){
            json.addProperty("newUsername", new_user);
        }

        if(!new_password.equals("")){
            json.addProperty("newPsw", new_password);
        }

        return json.toString();
    }

    ///Rende il messaggio di submitProposal
    public static String get_submitProposal_message(int id, List<String> words){
        JsonObject json = new JsonObject();
        json.addProperty("operation", "submitProposal");
        json.addProperty("puzzle_id", id);

        JsonArray array = new JsonArray();
        Iterator<String> iter = words.iterator();
        while(iter.hasNext()){
            array.add(iter.next());
        }

        json.add("words", array);

        return json.toString();
    }

    ///Rende il messaggio di requestGameInfo
    public static String get_requestGameInfo_message(int id){
        JsonObject json = new JsonObject();
        json.addProperty("operation", "requestGameInfo");
        json.addProperty("gameId", id);
        return json.toString();
    }

    ///Rende il messaggio di requestGameInfo
    public static String get_requestGameStats_message(int id){
        JsonObject json = new JsonObject();
        json.addProperty("operation", "requestGameStats");
        json.addProperty("gameId", id);
        return json.toString();
    }

    ///Rende il messaggio di requestLeaderboard
    public static String get_requestLeaderboard_message(String player_name, int num){
        JsonObject json = new JsonObject();
        json.addProperty("operation", "requestLeaderboard");
        json.addProperty("playerName", player_name);
        json.addProperty("topPlayers", num);
        return json.toString();
    }

    ///Rende il messaggio di requestPlayerStats
    public static String get_requestPlayerStats_message(){
        JsonObject json = new JsonObject();
        json.addProperty("operation", "requestPlayerStats");
        return json.toString();
    }

}

