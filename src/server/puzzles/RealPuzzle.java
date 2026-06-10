package server.puzzles;


import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class RealPuzzle {
    public AtomicBoolean is_current;
    public int id, partecipanti, conclusori, vincitori, time_left;
    public float media;
    private final Map<String, String> groups;
    public Map<String, List<String>> solution;

    public RealPuzzle(int id, Map<String,String> groups, int time_left){
        this.id = id;
        this.time_left = time_left;
        this.groups = groups;

        //inverto solutions da parola -> gruppo a gruppo -> parola
        this.solution = new HashMap<String, List<String>>();
        Iterator<String> keys = groups.keySet().iterator();
        while(keys.hasNext()){
            String word = keys.next();
            String name = groups.get(word);

            if(!solution.containsKey(name)){
                solution.put(name, new ArrayList<String>());
            }

            solution.get(name).add(word);
        }

        partecipanti = 0;
        conclusori = 0;
        vincitori = 0;
        media = 0;
        is_current = new AtomicBoolean(true);
    }

    public Map<String, String> getGroups() {
        return groups;
    }

    @Override
    public String toString(){
        String puzzle = "";
        if(is_current.get()){
            puzzle += "Tempo Rimasto: " + time_left + "ms\n";
        }
        else{
            puzzle += "Punteggio Medio: " + media + "\n";
        }
        puzzle += "Partecipanti: " + partecipanti + "\n";
        puzzle += "Conclusori: " + conclusori + "\n";
        puzzle += "Vincitori: " + vincitori + "\n";
        return puzzle;
    }
}
