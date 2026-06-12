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
        persistence_m = new PersistenceManager();
        user_m = new UserManager(this, persistence_m);

        //avvio il thread delle notifiche udp
        //implementa runnable
        udp_notifier = new UDPNotifier(user_m);
        new Thread(udp_notifier).start();


        game_m = new GameManager(this, user_m, udp_notifier, persistence_m);

        //avvio il threadpool delle sessioni tcp
        threadPool = Executors.newCachedThreadPool();



        //schedulo in modo periodico il l'aggiornamento dei file su disco
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(persistence_m::flush_all, 30, 60, TimeUnit.SECONDS);

        //questo thread diventa l'accettatore delle nuove connessioni
        acceptLoop();
    }

    private void acceptLoop() {

    }
}
