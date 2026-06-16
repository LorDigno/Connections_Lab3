package server.game;

import server.PersistenceManager;
import server.communication.UDPNotifier;
import server.users.UserManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameServer {
    //componenti di configurazione
    public int tcp_port, timeout, game_time;
    public List<String> banlist;

    //componenti principali
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

        //classe che gestisce le notifiche udp
        udp_notifier = new UDPNotifier();

        user_m = new UserManager(this, persistence_m, udp_notifier);
        game_m = new GameManager(this, user_m, udp_notifier, persistence_m);

        //schedulo in modo periodico il l'aggiornamento dei file su disco
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(persistence_m::flush_all, 60, 120, TimeUnit.SECONDS);

        //avvio la prima partita, le altre vengono da se
        game_m.launch();

        //questo thread diventa l'accettatore delle nuove connessioni
        acceptLoop();
    }

    private void acceptLoop() {
        try(ServerSocket sock = new ServerSocket(this.tcp_port)){
            //ciclo d'accettazione
            while(true){
                Socket tcp = sock.accept();
                game_m.submit_client(tcp);
            }

        }catch (IOException e){
            //tbd
        }
    }
}
