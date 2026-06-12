package server;

import server.puzzles.RealPuzzle;
import server.puzzles.UserPuzzle;
import server.users.UserManager;

import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

//gestisce effettivamente il puzzle
class GameManager {
    private volatile RealPuzzle current;
    private ConcurrentHashMap<Integer, UserPuzzle> participants;
    private ScheduledExecutorService timer;
    private GameDataLoader loader;
    private GameServer server;
    private UserManager users;
    private ExecutorService pool;
    private UDPNotifier udp_notifier;
    public AtomicBoolean changing_game;
    private PersistenceManager persistance;

    public GameManager(GameServer server, UserManager users, UDPNotifier udp_notifier, PersistenceManager pers){
        this.server = server;
        this.users = users;
        loader = new GameDataLoader();
        this.udp_notifier = udp_notifier;
        changing_game = new AtomicBoolean(false);
    }

    //supponiamo che sia stato tutto ripulito nel end_game precedente
    public void launch(){
        current = loader.load_new();

        pool = Executors.newCachedThreadPool();

        timer = Executors.newSingleThreadScheduledExecutor();
        timer.schedule(this::end_game, server.game_time, TimeUnit.MILLISECONDS);
    }

    //aggiunge una connessione accettata al threadpool
    public void submit_client(){
        //tbd, devo ancora capire come accettare le connessioni
    }

    //attiva il flush di tutto, cambia gli stati e invia la notifica udp
    private void end_game(){
        //le richieste che arrivano ora sono scadute
        changing_game.set(true);
        RealPuzzle old_game = current;

        //salvo la partita vecchia
        save();

        //creo la partita nuova
        current = loader.load_new();

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
        changing_game.set(false);
    }

    //avvia in qualche modo il salvataggio della partita
    public void save(){
        //tbd
    }
}