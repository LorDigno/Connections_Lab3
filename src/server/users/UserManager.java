package server.users;

import com.google.gson.Gson;
import server.game.GameServer;
import server.PersistenceManager;
import server.communication.ResponseStatus;
import server.communication.StatusDescription;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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

        load_from_disk();
    }

    public StatusDescription session_login(String username, String password,
                                           InetAddress address, int udp_port) {
        StatusDescription out = new StatusDescription();
        Integer id = user_id.get(username);

        //non c'è lo user
        if (id == null){
            out.setStatus(ResponseStatus.USER_NOT_FOUND);
            out.setDescription("Non esiste un profilo di nome: \"" + username + "\"");
            return out;
        }

        User user = users.get(id);
        if (!user.getPassword().equals(password)){
            out.setStatus(ResponseStatus.WRONG_PASSWORD);
            out.setDescription("La password inserita è errata");
            return out;
        }

        UserSession session = new UserSession(username, address, udp_port);
        //per garantire l'atomicità
        UserSession existing = active_sessions.putIfAbsent(id, session);

        if (existing != null) {
            //qualcun altro è arrivato prima
            out.setStatus(ResponseStatus.LOGIN_ALREADY_CONNECTED);
            out.setDescription("E' già attiva una sessione con l'utente \"" + username + "\"");
            return out;
        }

        out.setStatus(ResponseStatus.OK);
        out.setDescription("Login eseguito correttamente");
        return out;
    }

    public StatusDescription logout(int id){
        StatusDescription out = new StatusDescription();

        //rimuove la sessione atomicamente, null se non c'era
        UserSession removed = active_sessions.remove(id);

        if(removed == null){
            //non è loggato, dovrebbe essere impossibile fare logout da non logged però
            out.setStatus(ResponseStatus.NOT_LOGGED);
            out.setDescription("Non risulti loggato");

        }else{
            out.setStatus(ResponseStatus.OK);
            out.setDescription("logout effettuato");
        }

        return out;
    }

    //ritorna false se username già esistente
    public StatusDescription register(String username, String password){
        StatusDescription out = new StatusDescription();

        if(server.banlist.contains(username)){
            //username non valido
            out.setStatus(ResponseStatus.USERNAME_BANNED);
            out.setDescription("Username non accettabile");
            return out;
        }

        //per capire se c'è stata la lambda
        boolean[] wasCreated = new boolean[1];

        //computeIfAbsent e non putIfAbsent per via del User.next_id statico
        user_id.computeIfAbsent(username,
                k -> {
                    wasCreated[0] = true;

                    User newUser = new User(username, password);
                    users.put(newUser.getId(), newUser);

                    //da salvare
                    persistence.mark_user(newUser);
                    return newUser.getId();
                }
        );

        //rendo il payload in base alla creazione del nuovo user
        if(!wasCreated[0]){
            out.setStatus(ResponseStatus.USERNAME_TAKEN);
            out.setDescription("Username già registrato");
        }else{
            out.setStatus(ResponseStatus.OK);
            out.setDescription("Registrazione completata con successo");
        }

        return out;
    }

    public StatusDescription update_credentials(String username, String password,
                                      String new_username, String new_password){
        StatusDescription out = new StatusDescription();
        Integer id = user_id.get(username);
        if(id == null){
            out.setStatus(ResponseStatus.USER_NOT_FOUND);
            out.setDescription("Non esiste un profilo di nome: \"" + username +"\"");
            return out;
        }

        User user = users.get(id);

        if(!user.getPassword().equals(password)){
            //password sbagliata
            out.setStatus(ResponseStatus.WRONG_PASSWORD);
            out.setDescription("La password inserita è errata");
            return out;
        }

        //anche se dovrebbe essere sempre diverso dal vecchio
        if(new_username != null){
            if(server.banlist.contains(new_username)){
                out.setStatus(ResponseStatus.USERNAME_BANNED);
                out.setDescription("Username non accettabile");
                return out;
            }

            Integer existing = user_id.putIfAbsent(new_username, id);
            if(existing != null){
                out.setStatus(ResponseStatus.USERNAME_TAKEN);
                out.setDescription("Username già registrato");
                return out;
            }

            //cambio nella mappa, cambiato lo username l'entry vecchia non serve più
            user_id.remove(username);
        }

        //passati i controlli
        out.setStatus(ResponseStatus.OK);
        out.setDescription("OK");

        //setter synchronized sullo user
        user.update_credentials(new_username, new_password);

        //segnalo che è da flushare
        persistence.mark_user(user);

        return out;
    }

    public void puzzle_done(int id, int mistakes, int guesses_left, int right_ones){
        User user = users.get(id);

        //l'intero cambiamento dello stato deve essere atomico
        //l'unica lettura di questi campi è in UserFile.fill() che è synchronized
        synchronized(user){
            //caso incompleto
            if(guesses_left > 0){
                //incomplete era già stato aumentato al login
                user.curr_streak = 0;

            }else{
                //casi in cui la partita è giunta a compimento
                user.incomplete--;

                switch(mistakes){
                    case 0:
                        user.perfect++;
                        break;

                    case 1:
                        user.one_mistake++;
                        break;

                    case 2:
                        user.two_mistakes++;
                        break;

                    case 3:
                        user.three_mistakes++;
                        break;

                    case 4:
                        user.four_mistakes++;
                        break;
                }

                if(right_ones < 3){
                    user.failed++;
                    user.curr_streak = 0;
                }else{
                    user.wins++;
                    user.curr_streak += 1;
                }

                //aggiorno i rate
                user.win_rate = (float) user.wins / user.played;
                user.loss_rate = (float) user.failed / user.played;

                //update streak
                if(user.curr_streak > user.max_streak){
                    user.max_streak = user.curr_streak;
                }
            }
        }

        persistence.mark_user(user);
    }

    //renddo gli user loggati
    public Set<Integer> getActive_users() {
        return active_sessions.keySet();
    }

    public User get_user_from_id(int id){
        return users.get(id);
    }

    public int get_id_from_username(String username){
        return user_id.get(username);
    }

    //legge gli user già noti dal disco
    private void load_from_disk(){
        File dir = new File("data/users");

        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if(files == null) return;


        int max = -1;
        for(File file : files){
            User user = get_user_from_file(file);

            if(user != null){
                if(user.getId() > max){
                    max = user.getId();
                }

                users.put(user.getId(), user);
                user_id.put(user.getUsername(), user.getId());
            }
        }

        User.next_id.set(max + 1);
    }

    private User get_user_from_file(File file){
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(file)){
            //ottengo le info
            UserFile uf = gson.fromJson(reader, UserFile.class);

            //rendo lo user corrispondente
            return new User(uf);

        } catch(IOException e){
            System.err.println("Errore lettura file utente: " + file.getName());
            //tbd
            return null;
        }
    }
}
