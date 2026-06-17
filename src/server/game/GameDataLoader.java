package server.game;

import com.google.gson.stream.JsonReader;
import server.FatalServerException;
import server.puzzles.RealPuzzle;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameDataLoader {
    // streaming reader tenuto aperto tra una chiamata e l'altra
    private JsonReader reader;

    public GameDataLoader() throws FatalServerException{
        try{
            reader = new JsonReader(new FileReader("Connections_Data.json"));
            reader.beginArray();
        } catch(FileNotFoundException e) {
            close();
            throw new FatalServerException(e.getMessage());
        } catch(IOException e) {
            close();
            throw new FatalServerException(e.getMessage());
        }
    }

    //carica e restituisce la prossima partita dal file
    //restituisce null se il file è esaurito
    public RealPuzzle load_next() throws FatalServerException{
        try{
            //se c'è un prossimo elemento nell'array lo leggo
            if (reader.hasNext()) {
                return parse_game();
            }

            //array esaurito
            reader.endArray();
            close();
            return null;

        }catch(IOException e) {
            close();
            throw new FatalServerException(e.getMessage());
        }
    }

    //chiude il reader finito il file
    public void close(){
        try{
            if (reader != null) {
                reader.close();
                reader = null;
            }
        }catch(IOException e){
            System.err.println("Errore di chiusura dello stream dei dati sulle partite");
        }
    }

    private RealPuzzle parse_game() throws IOException{
        int id = -1;
        Map<String, String> groups = new HashMap<>();

        reader.beginObject();
        while (reader.hasNext()){
            String field = reader.nextName();

            switch (field){
                case "gameId":
                    //prendo l'intero
                    id = reader.nextInt();
                    break;

                case "groups":
                    //creo l'array dei gruppi
                    reader.beginArray();

                    while (reader.hasNext()){
                        // leggo un singolo gruppo
                        String theme = null;
                        List<String> words = new ArrayList<>();

                        reader.beginObject();
                        while (reader.hasNext()){
                            String group_field = reader.nextName();

                            switch (group_field) {
                                case "theme":
                                    theme = reader.nextString();
                                    break;

                                case "words":
                                    reader.beginArray();
                                    while (reader.hasNext()){
                                        words.add(reader.nextString());
                                    }
                                    //finisce il gruppo corrente
                                    reader.endArray();
                                    break;

                                default:
                                    reader.skipValue();
                            }
                        }
                        reader.endObject();

                        //costruisco la mappa word -> theme
                        for(String word : words){
                            groups.put(word, theme);
                        }
                    }
                    reader.endArray();
                    break;

                default:
                    reader.skipValue();
            }
        }
        reader.endObject();

        return new RealPuzzle(id, groups);
    }
}
