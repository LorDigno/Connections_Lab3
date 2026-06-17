package server.game;

import server.FatalServerException;
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
    public final int tcp_port, timeout, game_time, flush_delay, flush_time;
    public final List<String> banlist;

    //componenti principali
    private ScheduledExecutorService scheduler;
    private UDPNotifier udp_notifier;
    private GameManager game_m;
    private UserManager user_m;
    private PersistenceManager persistence_m;
    private ServerSocket sock;

    public GameServer(int tcp_port, int timeout, List<String> banlist, int game_time,
                      int flush_delay, int flush_time){
        this.tcp_port = tcp_port;
        this.timeout = timeout;
        this.banlist = banlist;
        this.game_time = game_time;
        this.flush_delay = flush_delay;
        this.flush_time = flush_time;
    }

    public void launch(){
        try{
            //classe che gestisce le notifiche udp
            udp_notifier = new UDPNotifier();

            //schedulo in modo periodico il l'aggiornamento dei file su disco
            persistence_m = new PersistenceManager();
            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(persistence_m::flush_all, flush_delay, flush_time, TimeUnit.SECONDS);

            //hestione utenti e partite
            user_m = new UserManager(this, persistence_m, udp_notifier);
            game_m = new GameManager(this, user_m, udp_notifier, persistence_m);

            //avvio la prima partita, le altre vengono da se
            game_m.launch();

            //questo thread diventa l'accettatore delle nuove connessioni
            try{
                sock = new ServerSocket(this.tcp_port);
            }catch(IOException e){
                throw new FatalServerException("Non si apre il serversocket");
            }

            acceptLoop();
        }catch (FatalServerException e){
            System.err.println(e.getMessage());
            server_shutdown();
        }
    }

    private void acceptLoop() throws FatalServerException{
        try{
            //ciclo d'accettazione
            while(true){
                Socket tcp = sock.accept();
                game_m.submit_client(tcp);
            }
        }catch(IOException e){
            //il socket è corrotto o si è chiuso volontariamente
            server_shutdown();
        }
    }

    ///Chiude forzatamente il socket così da spegnere il server
    public void stop_loop(){
        try{
            sock.close();
        }catch(IOException e){
            System.err.println("Non si chiude il serversocket");
        }
    }

    private void server_shutdown(){
        if(game_m != null){
            game_m.close_all();
        }

        //aggiungi poi quelli degli altri componenti se necessari
    }
}
