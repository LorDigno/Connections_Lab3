package server.users;

import com.google.gson.Gson;
import server.communication.UDPNotifier;
import server.game.GameServer;
import server.PersistenceManager;
import server.communication.ResponseStatus;
import server.communication.StatusDescription;
import server.puzzles.UserPuzzleData;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

///Gestisce le sessioni utente e tutti i dati sugli utenti stessi
public class UserManager {
    //utenti recuperati dal disco e sessioni attive
    private final ConcurrentHashMap<Integer, User> users;
    private final ConcurrentHashMap<Integer, UserSession> active_sessions;
    private final ConcurrentHashMap<String, Integer> user_id;
    private PersistenceManager persistence;
    private UDPNotifier udpNotifier;
    private GameServer server;

    public UserManager(GameServer server, PersistenceManager pers, UDPNotifier udp){
        users = new ConcurrentHashMap<>();
        active_sessions = new ConcurrentHashMap<>();
        user_id = new ConcurrentHashMap<>();
        this.server = server;
        this.persistence = pers;
        this.udpNotifier = udp;

        load_from_disk();
    }

    ///Registra una nuova sessione utente
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
        out.setDescription("Login eseguito correttamente.\n " +
                "Puoi vedere la partita con requestGameInfo della partita corrente\n");
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

        //può crearsi comunque l'incosistenza se logga durante il cambio di credenziali
        if(active_sessions.containsKey(id)){
            out.setStatus(ResponseStatus.UPDATE_ACTIVE_USER);
            out.setDescription("Non si può cambiare le credenziali di un utente attualmente in gioco");
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

        //se si è creato uno stato incosistente nel client lo kicko
        if(active_sessions.containsKey(id)){
            udpNotifier.kick(active_sessions.get(id));
        }

        return out;
    }

    ///Una volta completato un puzzle (o per timeout) aggiorna i dati dell'utente con quelli dello UserPuzzle
    public void puzzle_done(int id, int mistakes, int guesses_left, int right_ones, int score){
        User user = users.get(id);

        //l'intero cambiamento dello stato deve essere atomico
        //l'unica lettura di questi campi è in UserFile.fill() che è synchronized
        synchronized(user){
            //aggiungo il punteggio
            user.total_score += score;

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

    ///Rende tutti gli user loggati
    public Set<Integer> getActive_users() {
        return active_sessions.keySet();
    }

    ///Rimuove una sessione utente
    public void remove_active(int id){
        active_sessions.remove(id);
    }

    ///Rende l'oggetto user dal suo id
    public User get_user_from_id(int id){
        return users.get(id);
    }

    ///Rende l'id di uno user dal suo username
    public int get_id_from_username(String username){
        return user_id.get(username);
    }

    ///Legge gli user già noti dal disco, chiamato solo all'avvio, utile in caso di riavvio
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

    ///Fai il parsing di un file in UserFile e poi in User con Gson
    private User get_user_from_file(File file){
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(file)){
            //ottengo le info
            UserFile uf = gson.fromJson(reader, UserFile.class);

            //rendo lo user corrispondente
            return new User(uf);

        } catch(IOException e){
            System.err.println("Errore lettura file utente: " + file.getName());
            return null;
        }
    }

    ///Fai il parsing di un file e recupera lo UserPuzzleData con Gson
    public UserPuzzleData get_user_puzzle(int user_id, int puzzle_id){
        File file = new File("data/users/" + user_id + ".json");
        if(!file.exists()) return null;

        Gson gson = new Gson();
        try(FileReader reader = new FileReader(file)){
            //cerco la partita giusta
            UserFile uf = gson.fromJson(reader, UserFile.class);
            return uf.partite.get(String.valueOf(puzzle_id));

        } catch(IOException e){
            //vuol dire che non c'è la partita
            return null;
        }
    }

    public StatusDescription get_player_stats(int id){
        StatusDescription out = new StatusDescription();
        if(id == -1 || !users.containsKey(id)){
            out.setStatus(ResponseStatus.NOT_LOGGED);
            out.setDescription("Devi prima accedere");
            return out;
        }

        //come al solito non si sa mai
        if(!users.containsKey(id)){
            out.setStatus(ResponseStatus.USER_NOT_FOUND);
            out.setDescription("Lo user non esite");
            return out;
        }

        User u = users.get(id);
        out.setStatus(ResponseStatus.OK);
        out.setDescription(u.get_stats());
        return out;
    }

    ///Calcola la classifica dei primi num utenti oppure relativa con who
    public StatusDescription get_overall_leaderboard(String who, int num, int me){
        StatusDescription out = new StatusDescription();
        if(me == -1 || !users.containsKey(me)){
            out.setStatus(ResponseStatus.NOT_LOGGED);
            out.setDescription("Devi prima accedere");
            return out;
        }

        //caso in cui voglia un giocatore specifico
        if(!who.equals("none")){
            if(!user_id.containsKey(who) || !users.containsKey(user_id.get(who))){
                out.setStatus(ResponseStatus.USER_NOT_FOUND);
                out.setDescription("L'utente di nome \"" + who + "\" non esiste");
                return out;
            }

            User user = users.get(user_id.get(who));
            User myself = users.get(me);

            int ms = myself.get_score(), us = user.get_score();
            if(ms < us){
                out.setStatus(ResponseStatus.OK);
                String desc = "Giocatore: \"" + user.getUsername() + "\" ha punteggio: " + us
                        + " maggiore del giocatore: \"" + myself.getUsername() + "\" con punteggio: "
                        + ms + "\n";
                out.setDescription(desc);
            }else if(ms > us){
                out.setStatus(ResponseStatus.OK);
                String desc = "Giocatore: \"" + myself.getUsername() + "\" ha punteggio: " + ms
                        + " maggiore del giocatore: \"" + user.getUsername() + "\" con punteggio: "
                        + us + "\n";
                out.setDescription(desc);
            }else{
                out.setStatus(ResponseStatus.OK);
                String desc = "Giocatore: \"" + myself.getUsername() + "\" ha punteggio: " + ms
                        + " pari al giocatore: \"" + user.getUsername() + "\" con punteggio: "
                        + us + "\n";
                out.setDescription(desc);
            }

            return out;
        }

        //caso in cui devo calcolare la classifica dei migliori num (num = 0 allora tutti)
        List<User> leaderboard = new ArrayList<>(users.values());
        leaderboard.sort((u1, u2) -> Integer.compare(u2.get_score(), u1.get_score()));

        //se è maggiore della size li faccio comunque tutti
        if(num > 0 && num < leaderboard.size()){
            leaderboard = leaderboard.subList(0, num);
        }

        String desc = "";
        int pos = 1;
        for(User u : leaderboard){
            desc += pos++ + ". " + u.getUsername() + " - " + u.get_score() + " punti\n";
        }

        out.setStatus(ResponseStatus.OK);
        out.setDescription(desc);
        return out;
    }

    public Set<UserSession> get_active_sessions(){
        return new HashSet<>(active_sessions.values());
    }
}
