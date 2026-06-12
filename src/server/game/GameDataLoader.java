package server.game;

import com.google.gson.stream.JsonReader;
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
    private int games_loaded, game_time;

    public GameDataLoader(int game_time){
        this.game_time = game_time;
        games_loaded = 0;

        try{
            reader = new JsonReader(new FileReader("data/Connections_Data.json"));
            reader.beginArray();
        } catch (FileNotFoundException e) {
            //tbd
            throw new RuntimeException(e);
        } catch (IOException e) {
            //tbd
            throw new RuntimeException(e);
        }
    }

    //carica e restituisce la prossima partita dal file
    //restituisce null se il file è esaurito
    public RealPuzzle load_next(){
        try{
            //se c'è un prossimo elemento nell'array lo leggo
            if (reader.hasNext()) {
                games_loaded++;
                return parse_game();
            }

            //array esaurito
            reader.endArray();
            close();
            return null;

        } catch (Exception e) {
            //tbd
            throw new RuntimeException(e);
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
            //tbd
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

        return new RealPuzzle(id, groups, game_time);
    }
}
