package server;

import server.game.GameServer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Properties;


public class ServerMain {
    public static void main(String args[]){
        //inizializza l'oggetto GameServer leggendo la configurazione
        File config_file = new File("src/server/config.properties");
        try{
            FileReader reader = new FileReader(config_file);
            //parsing con properties
            Properties props = new Properties();
            props.load(reader);

            int tcp_port = Integer.parseInt(props.getProperty("tcp_port"));
            int timeout = Integer.parseInt(props.getProperty("timeout"));
            List<String> banlist = List.of(props.getProperty("banlist").split(";"));
            int game_time = Integer.parseInt(props.getProperty("game_time")); //in ms

            reader.close();
            //fine parsing

            GameServer server = new GameServer(tcp_port, timeout, banlist, game_time);
            server.launch();

        } catch (FileNotFoundException ex) {
            System.err.println("Non trovato il file di configurazione");
            System.exit(1);
        } catch (IOException ex) {
            System.err.println("Problemi durante la configurazione");
            System.exit(1);
        }
    }
}

