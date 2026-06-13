package server.game;

import com.google.gson.Gson;
import server.PersistenceManager;
import server.communication.ClientHandler;
import server.communication.ResponseStatus;
import server.communication.StatusDescription;
import server.puzzles.RealPuzzle;
import server.puzzles.RealPuzzleFile;
import server.puzzles.UserPuzzle;
import server.puzzles.UserPuzzleData;
import server.users.User;
import server.users.UserManager;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

//gestisce effettivamente il puzzle
public class GameManager{
    private volatile RealPuzzle current;
    private ConcurrentHashMap<Integer, UserPuzzle> participants;
    private ScheduledExecutorService timer;
    private GameDataLoader loader;
    private GameServer server;
    private UserManager users;
    private ExecutorService pool;
    private UDPNotifier udp_notifier;
    private volatile long start_time;

    //monitor per endgame
    private final Object game_lock;
    private volatile boolean changing_game = false;
    private volatile int active_requests = 0;

    private PersistenceManager persistance;

    public GameManager(GameServer server, UserManager users, UDPNotifier udp_notifier, PersistenceManager pers){
        this.server = server;
        this.users = users;
        loader = new GameDataLoader();
        this.udp_notifier = udp_notifier;
        this.persistance = pers;

        game_lock = new Object();
        changing_game =  false;
        active_requests = 0;
        start_time = 0;

        participants = new ConcurrentHashMap<>();
    }

    //supponiamo che sia stato tutto ripulito nel end_game precedente
    public void launch(){
        current = loader.load_next();
        start_time = System.currentTimeMillis();
        pool = Executors.newCachedThreadPool();

        timer = Executors.newSingleThreadScheduledExecutor();
        timer.schedule(this::end_game, server.game_time, TimeUnit.MILLISECONDS);
    }

    //aggiunge una connessione accettata al threadpool
    public void submit_client(Socket tcp_sock){
        //tbd, devo ancora capire come accettare le connessioni
        pool.submit(new ClientHandler(tcp_sock, this, users));
    }

    //attiva il flush di tutto, cambia gli stati e invia la notifica udp
    private void end_game(){
        //le richieste che arrivano ora sono scadute
        synchronized (game_lock){
            changing_game = true;

            //attesa passiva sulla lock come condition variable
            while (active_requests > 0) {
                try {
                    game_lock.wait();
                } catch (InterruptedException e) {
                    //tbd
                    Thread.currentThread().interrupt();
                }
            }
        }

        current.is_current.set(false);

        //salvo le implicazioni delle partite finite
        for(Integer i: participants.keySet()){
            //solo di quelli di cui non si è già fatto game_over
            if(!participants.get(i).is_finished()){
                game_over(i, participants.get(i));
            }
        }

        String leaderboard = get_current_leaderboard();

        //creo la partita nuova
        RealPuzzle old_game = current;
        current = loader.load_next();
        start_time = System.currentTimeMillis();

        //notifico gli utenti attivi della fine della partita
        udp_notifier.notify(old_game, current, leaderboard);
        timer.schedule(this::end_game, server.game_time, TimeUnit.MILLISECONDS);

        //aggiorno participants agli user attivi
        participants.clear();
        HashSet<Integer> logged = new HashSet<Integer>(users.getActive_users());
        Iterator<Integer> iter = logged.iterator();
        while(iter.hasNext()){
            int u = iter.next();
            //se siamo qua u esiste per forza.
            participants.put(u, new UserPuzzle(users.get_user_from_id(u), current));
        }

        //le richieste ritornano valide
        changing_game = false;
        persistance.flush_all();

        //adesso che sono stati flushati quelli vecchi posso mettere in cache quelli nuovi
        for(Integer u: participants.keySet()){
            persistance.mark_user_puzzle(participants.get(u));
        }
    }

    //per il conteggio delle operazioni a mezzo che bloccano endgame
    public boolean try_start_action(){
        synchronized (game_lock){
            //accetti solo se non sta cambiando la partita
            if (changing_game) {
                return false;
            }

            active_requests++;
            return true;
        }
    }
    public void end_action() {
        synchronized (game_lock) {
            active_requests--;

            if (active_requests == 0 && changing_game) {
                //sveglio endgame
                game_lock.notifyAll();
            }
        }
    }

    public void log_participant(int user_id){
        participants.computeIfAbsent(user_id,
                id -> {
                    User u = users.get_user_from_id(id);

                    //ha giocato una partita in più
                    u.add_game();

                    //dirty
                    persistance.mark_user(u);

                    //creo e segno come dirty lo userpuzzle nuovo
                    UserPuzzle up = new UserPuzzle(u, current);
                    persistance.mark_user_puzzle(up);

                    //aggiungo un partecipante alla partita
                    current.add_participant();

                    return up;
                });
    }

    public int current_id(){
        return current.id;
    }

    public StatusDescription submit_proposal(int user_id, int puzzle_id, List<String> words){
        StatusDescription out;
        UserPuzzle up = participants.get(user_id);

        //non dovrebbe essere possibile ma controllo comunque
        if(up == null){
            out = new StatusDescription();
            out.setStatus(ResponseStatus.NOT_LOGGED);
            out.setDescription("Non è possibile giocare da sloggati");
            return  out;
        }

        if(puzzle_id != current_id()){
            out = new StatusDescription();
            out.setStatus(ResponseStatus.PROPOSAL_OLD_GAME);
            out.setDescription("La proposta inviata è relativa ad una partita precedente");
            return  out;
        }

        //controllo che non sia già finito
        if(up.is_finished()){
            out = new StatusDescription();
            out.setStatus(ResponseStatus.PROPOSAL_ALREADY_PLAYED);
            out.setDescription("Hai già terminato la partita attuale, aspetta la prossima");
            return  out;
        }

        out = up.analyze(words);

        if(out.getStatus() == ResponseStatus.OK){
            persistance.mark_user_puzzle(up);
        }

        if(up.is_finished()){
            game_over(user_id, up);

            //aggiungo la classifica alla descrizione
            out.setDescription(out.getDescription()
                    + "\nLa classifica attuale è:\n" + get_current_leaderboard());
        }

        return out;
    }

    private void game_over(int user_id, UserPuzzle up){
        //cambio i dati dell'utente
        users.puzzle_done(user_id, up.mistakes, up.guesses_left, up.right_ones);

        //cambio i dati del realgame
        synchronized(current){
            if(up.is_finished()){
                current.finished++;

                if(up.right_ones == 3){
                    current.winners++;
                }
            }

            current.total_score += up.score;

        }

        persistance.mark_game(current);
    }

    //genera la classifica attuale della partita in corso
    public String get_current_leaderboard() {
        List<UserPuzzle> leaderboard = new ArrayList<>();
        for (UserPuzzle puzzle : participants.values()) {
            leaderboard.add(puzzle);
        }

        Collections.sort(leaderboard,
                new Comparator<UserPuzzle>() {
                    @Override
                    public int compare(UserPuzzle p1, UserPuzzle p2) {
                        //se p2 ha più punti, torna numero positivo (p2 sale)
                        //se p1 ha più punti, torna numero negativo (p1 sale)
                        return Integer.compare(p2.score, p1.score);
                    }
                }
        );

        String out = "";
        for(UserPuzzle up: leaderboard){
            out += up.user.getUsername() + " ha totalizzato: " + up.score + " punti\n";
        }

        return out;
    }

    public StatusDescription get_puzzle_info(int user_id, int puzzle_id){
        StatusDescription out = new StatusDescription();
        UserPuzzle up = participants.get(user_id);

        if(up == null){
            out.setStatus(ResponseStatus.NOT_LOGGED);
            out.setDescription("Devi essere loggato per vedere i dati delle partite");
            return  out;
        }

        if(puzzle_id == -1 || puzzle_id == current_id()){
            out.setStatus(ResponseStatus.OK);

            if(up.is_finished()){
                out.setDescription(up.get_puzzle_stats());
                return out;
            }

            //allora gli rendo il corrente
            String desc = up.get_puzzle_state();
            desc += "Tempo Rimasto: " + get_time_left() + "ms\n";
            out.setDescription(desc);
            return out;
        }

        //non è il corrente
        //va cercato nel file data/users/id.json
        UserPuzzleData upd = users.get_user_puzzle(user_id, puzzle_id);

        if(upd == null){
            out.setStatus(ResponseStatus.INFO_GAME_NOT_PLAYED);
            out.setDescription("La partita richiesta non esiste o non è stata giocata");
            return out;
        }

        up = new UserPuzzle(upd);
        out.setStatus(ResponseStatus.OK);
        out.setDescription(up.get_puzzle_stats());

        return out;
    }

    public StatusDescription get_puzzle_stats(int puzzle_id){
        StatusDescription out = new StatusDescription();

        if(puzzle_id == -1 || puzzle_id == current_id()){
            //allora gli rendo il corrente
            String desc = current.get_stats();
            desc += "Tempo Rimasto: " + get_time_left() + "ms\n";
            out.setDescription(desc);
            return out;
        }

        //devo cercare lo storico
        RealPuzzleFile rpf = get_finished_game(puzzle_id);
        if(rpf == null){
            out.setStatus(ResponseStatus.STATS_GAME_NOT_FOUND);
            out.setDescription("La partita di id: \"" + puzzle_id + "\" non esiste");
            return out;
        }

        //posso creare il realpuzzle corrispondente
        RealPuzzle rp = new RealPuzzle(rpf);
        out.setStatus(ResponseStatus.OK);
        out.setDescription(rp.get_stats());
        return out;
    }

    private RealPuzzleFile get_finished_game(int id){
        File file = new File("data/puzzles/" + id + ".json");
        if(!file.exists()) return null;

        Gson gson = new Gson();
        try(FileReader reader = new FileReader(file)){
            return gson.fromJson(reader, RealPuzzleFile.class);
        } catch(IOException e){
            return null;
        }
    }

    private int get_time_left(){
        //in alcuni casi limite può venire negativo
        return (int) Math.max(0,server.game_time - (System.currentTimeMillis() -  start_time));
    }

}