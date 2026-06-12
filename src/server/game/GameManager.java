package server.game;

import server.PersistenceManager;
import server.communication.ClientHandler;
import server.puzzles.RealPuzzle;
import server.puzzles.UserPuzzle;
import server.users.User;
import server.users.UserManager;

import java.net.Socket;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

//gestisce effettivamente il puzzle
public class GameManager {
    private volatile RealPuzzle current;
    private ConcurrentHashMap<Integer, UserPuzzle> participants;
    private ScheduledExecutorService timer;
    private GameDataLoader loader;
    private GameServer server;
    private UserManager users;
    private ExecutorService pool;
    private UDPNotifier udp_notifier;

    //monitor per endgame
    private final Object game_lock;
    private boolean changing_game = false;
    private int active_requests = 0;

    private PersistenceManager persistance;

    public GameManager(GameServer server, UserManager users, UDPNotifier udp_notifier, PersistenceManager pers){
        this.server = server;
        this.users = users;
        loader = new GameDataLoader(server.game_time);
        this.udp_notifier = udp_notifier;

        game_lock = new Object();
        changing_game =  false;
        active_requests = 0;

        participants = new ConcurrentHashMap<>();
    }

    //supponiamo che sia stato tutto ripulito nel end_game precedente
    public void launch(){
        current = loader.load_next();
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
        synchronized (game_lock) {
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

        //avvio la terminazione
        RealPuzzle old_game = current;

        //salvo la partita vecchia
        save();

        //creo la partita nuova
        current = loader.load_next();

        //notifico gli utenti attivi della fine della partita
        udp_notifier.notify(old_game, current);
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

    //avvia in qualche modo il salvataggio della partita
    public void save(){
        //tbd
    }

    // in GameManager
    public UserPuzzle get_participant(int user_id) {
        return participants.get(user_id);
    }

    public void log_participant(int user_id){
        UserPuzzle up = new UserPuzzle(users.get_user_from_id(user_id), current);
        participants.putIfAbsent(user_id, up);
    }

}