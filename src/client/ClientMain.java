package client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ClientMain {
    //inizializza l'oggetto GameClient leggendo la configurazione
    public static void main(String args[]){
        File config_file = new File("src/Client/config.properties");

        try{
            FileReader reader = new FileReader(config_file);

            //parsing con properties
            Properties props = new Properties();
            props.load(reader);

            String host = props.getProperty("host");
            int tcp_port = Integer.parseInt(props.getProperty("tcp_port"));
            int udp_port = Integer.parseInt(props.getProperty("udp_port"));
            int timeout = Integer.parseInt(props.getProperty("timeout"));
            List<String> banlist = List.of(props.getProperty("banlist").split(";"));

            reader.close();
            //fine parsing

            //inizializza l'handler udp delle notifiche
            ExecutorService handler = Executors.newSingleThreadExecutor();
            handler.submit(new UdpHandler(udp_port, timeout, Thread.currentThread()));

            //inizializza il GameClient e lo lancia.
            GameClient game = new GameClient(host, tcp_port, timeout, banlist);
            System.out.println("Benvenuto in connections");
            game.launch();

            //terminazione dell'handler
            //vado direttamente di shutdownNow perchè è in while(true)
            handler.shutdownNow();

        } catch (FileNotFoundException ex) {
            System.err.println("Config.properties file not found");
            System.exit(1);
        } catch (IOException ex) {
            System.err.println("Exception during configuration parsing");
            System.exit(1);
        }


    }
}
