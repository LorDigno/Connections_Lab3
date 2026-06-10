package server;

import server.users.UserManager;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameServer {
    //componenti di configurazione
    public int tcp_port, timeout, game_time;
    public List<String> banlist;

    //componenti principali
    private ExecutorService threadPool;
    private ScheduledExecutorService scheduler;
    private UDPNotifier udp_notifier;
    private GameManager game_m;
    private UserManager user_m;
    private PersistenceManager persistence_m;

    public GameServer(int tcp_port, int timeout, List<String> banlist, int game_time){
        this.tcp_port = tcp_port;
        this.timeout = timeout;
        this.banlist = banlist;
        this.game_time = game_time;
    }

    public void launch(){
        //inizializzo i manager degli utenti, partite e della persistenza
        user_m = new UserManager(this);
        game_m = new GameManager();
        persistence_m = new PersistenceManager(user_m, game_m);

        //avvio il threadpool delle sessioni tcp
        threadPool = Executors.newCachedThreadPool();

        //avvio il thread delle notifiche udp
        udp_notifier = new UDPNotifier(game_m, user_m);
        new Thread(udp_notifier, "udp-notifier").start();

        //schedulo in modo periodico il l'aggiornamento dei file su disco
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(persistence_m::flush, 30, 60, TimeUnit.SECONDS);


        //questo thread diventa l'accettatore delle nuove connessioni
        acceptLoop();
    }

    private void acceptLoop() {

    }
}
