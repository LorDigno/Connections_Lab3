package client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;


public class ClientMain {
    //inizializza l'oggetto GameClient leggendo la configurazione
    public static void main(String args[]){
        File config_file = new File("src/client/config.properties");

        try{
            FileReader reader = new FileReader(config_file);

            //parsing con properties
            Properties props = new Properties();
            props.load(reader);

            String host = props.getProperty("host");
            int tcp_port = Integer.parseInt(props.getProperty("tcp_port"));
            int timeout = Integer.parseInt(props.getProperty("timeout"));
            List<String> banlist = List.of(props.getProperty("banlist").split(";"));

            reader.close();
            //fine parsing

            //costrutti di sincronizzazione dell'input
            BlockingQueue<String> queue = new LinkedBlockingDeque<String>();
            AtomicBoolean rejected = new AtomicBoolean(false);

            //inizializza l'InputDaemon
            Thread input_daemon = new Thread(new InputDaemon(queue, rejected));
            input_daemon.setDaemon(true);
            input_daemon.start();

            //inizializza il GameClient e lo lancia.
            GameClient game = new GameClient(host, tcp_port, timeout, banlist, queue, rejected);
            System.out.println("Benvenuto in connections");
            game.launch();

        } catch (FileNotFoundException ex) {
            System.err.println("Non trovato il file di configurazione");
            System.exit(1);
        } catch (IOException ex) {
            System.err.println("Problemi durante la configurazione");
            System.exit(1);
        }


    }
}
