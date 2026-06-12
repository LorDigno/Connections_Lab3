package server.users;

import server.GameServer;
import server.PersistenceManager;
import server.Status;
import server.StatusDescription;

import java.net.InetAddress;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class UserManager {
    //utenti recuperati dal disco e sessioni attive
    private final ConcurrentHashMap<Integer, User> users;
    private final ConcurrentHashMap<Integer, UserSession> active_sessions;
    private final ConcurrentHashMap<String, Integer> user_id;
    private PersistenceManager persistence;

    private GameServer server;

    public UserManager(GameServer server, PersistenceManager pers){
        users = new ConcurrentHashMap<>();
        active_sessions = new ConcurrentHashMap<>();
        user_id = new ConcurrentHashMap<>();
        this.server = server;
        this.persistence = pers;
    }

    //chiamato da
    //controlla le credenziali date
    public StatusDescription chack_login(String username, String password){
        User user = users.get(user_id.get(username));
        StatusDescription out = new StatusDescription();

        //utente non esiste
        if (user == null){
            out.setStatus(Status.LOGIN_USER_NOT_FOUND);
            out.setDescription("Non esiste un profilo di nome: \"" + username +"\"");
        } else if (!user.getPassword().equals(password)){
            //password sbagliata
            out.setStatus(Status.LOGIN_WRONG_PASSWORD);
            out.setDescription("La password inserita è errata");
        } else if (active_sessions.containsKey(user_id.get(username))){
            //già loggato
            out.setStatus(Status.LOGIN_ALREADY_CONNECTED);
            out.setDescription("E' già attiva una sessione con l'utente \""+ username + "\"");
        }else{
            out.setStatus(Status.OK);
            out.setDescription("Login eseguito correttamente");
        }

        return out;
    }

    //aggiunge e rende la sessione, da per scontato che sia stato gia chiamato check_login
    public UserSession session_login(String username, InetAddress address, int udp_port){
        UserSession out = new UserSession(username, address, udp_port);
        active_sessions.put(user_id.get(username), out);
        return out;
    }

    public StatusDescription logout(String username){
        StatusDescription out = new StatusDescription();

        if(!active_sessions.containsKey(user_id.get(username))){
            //non è loggato, dovrebbe essere impossibile fare logout da non logged però
            out.setStatus(Status.LOGOUT_NOT_LOGGED);
            out.setDescription("Lo username \"" + username + "\" non risulta loggato");

        }else{
            out.setStatus(Status.OK);
            out.setDescription("logout effettuato");

            //elimino la sessione
            active_sessions.remove(user_id.get(username));
        }

        return out;
    }

    //ritorna false se username già esistente
    public StatusDescription register(String username, String password){
        StatusDescription out = new StatusDescription();

        if(server.banlist.contains(username)){
            //username non valido
            out.setStatus(Status.REGISTER_USERNAME_BANNED);
            out.setDescription("Username non accettabile");

        } else if(user_id.containsKey(username)){
            //username già registrato
            out.setStatus(Status.REGISTER_USERNAME_TAKEN);
            out.setDescription("Username già registrato");

        }else{
            out.setStatus(Status.OK);
            out.setDescription("OK");

            //aggiunta del nuovo utente
            User user = new User(username, password);
            users.put(user.getId(), user);
            //trova il modo di aggiungerlo al disco (o nella coda per la persistenza)
            //tbd
        }

        return out;
    }

    public StatusDescription update_credentials(String username, String password,
                                      String new_username, String new_password){
        StatusDescription out = new StatusDescription();
        User user = users.get(user_id.get(username));

        if(user == null){
            //username non noto
            out.setStatus(Status.UPDATE_USER_NOT_FOUND);
            out.setDescription("Non esiste un profilo di nome: \"" + username +"\"");

        } else if(!user.getPassword().equals(password)){
            //password sbagliata
            out.setStatus(Status.UPDATE_WRONG_PASSWORD);
            out.setDescription("La password inserita è errata");

        } else if(new_username != null && user_id.containsKey(new_username)){
            //username già registrato
            out.setStatus(Status.UPDATE_USERNAME_TAKEN);
            out.setDescription("Username già registrato");

        } else if (new_username != null  && server.banlist.contains(new_username)){
            //username non valido
            out.setStatus(Status.UPDATE_USERNAME_BANNED);
            out.setDescription("Username non accettabile");

        }else {
            out.setStatus(Status.OK);
            out.setDescription("OK");

            //cambio l'effettivo utente
            if(new_username != null){
                user.setUsername(new_username);
            }

            if(new_password != null){
                user.setPassword(new_password);
            }

            //cambio nella mappa
            user_id.remove(username);
            user_id.put(user.getUsername(), user.getId());

            //segnalo che è da flushare
            //tbd
        }

        return out;
    }

    //per PersistenceManager
    public Collection<User> getAllUsers() {
        return users.values();
    }

    //per UDPNotifier
    public Collection<UserSession> getActive_sessions() {
        return active_sessions.values();
    }

    //renddo gli user loggati
    public Set<Integer> getActive_users() {
        return active_sessions.keySet();
    }

    public User get_user_from_id(int id){
        return users.get(id);
    }

    //legge gli user già noti dal disco
    private void loadFromDisk() {}
}
