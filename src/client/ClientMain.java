package client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

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
            int port = Integer.parseInt(props.getProperty("port"));
            int timeout = Integer.parseInt(props.getProperty("timeout"));
            String unit = props.getProperty("time_unit");
            List<String> banlist = List.of(props.getProperty("banlist").split(";"));

            reader.close();
            //fine parsing

            //inizializza il GameClient e lo lancia.
            GameClient game = new GameClient(host, port, timeout, banlist);
            game.launch();
        } catch (FileNotFoundException ex) {
            System.err.println("Config.properties file not found");
            System.exit(1);
        } catch (IOException ex) {
            System.err.println("Exception during configuration parsing");
            System.exit(1);
        }


    }
}
