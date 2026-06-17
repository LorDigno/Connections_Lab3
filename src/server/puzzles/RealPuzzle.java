package server.puzzles;


import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class RealPuzzle {
    public AtomicBoolean is_current;
    public int id, participants, finished, winners, time_left, total_score;;
    private final Map<String, String> groups;
    public Map<String, List<String>> solution;

    public RealPuzzle(int id, Map<String,String> groups){
        this.id = id;
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

        participants = 0;
        finished = 0;
        winners = 0;
        total_score = 0;
        is_current = new AtomicBoolean(true);
    }

    public RealPuzzle(RealPuzzleFile rpf){
        this.participants = rpf.partecipants;
        this.id = rpf.id;
        this.winners = rpf.winners;
        this.finished = rpf.finished;
        this.total_score = rpf.total_score;

        groups = null;
        is_current = new AtomicBoolean(false);
    }

    public Map<String, String> getGroups() {
        return groups;
    }

    public synchronized void add_participant(){
        participants++;
    }

    public synchronized String get_stats(){
        String puzzle = "";
        if(!is_current.get()){
            if(participants == 0){
                puzzle += "Punteggio Medio: " + 0 + "\n";
            }else{
                puzzle += "Punteggio Medio: " + (float)total_score/ participants + "\n";
            }

        }
        puzzle += "Partecipanti: " + participants + "\n";
        puzzle += "Conclusori: " + finished + "\n";
        puzzle += "Vincitori: " + winners + "\n";
        return puzzle;
    }

    public String toString(){
        return id + ";" + participants + ";" + total_score;
    }
}
