package server.puzzles;


import java.util.Map;

public class RealPuzzle {
    public boolean is_current;
    public int id, partecipanti, conclusori, vincitori, time_left;
    public float media;
    private final Map<String, String> solution;

    public RealPuzzle(int id, Map<String,String> groups, int time_left){
        this.id = id;
        this.time_left = time_left;
        solution = groups;
        partecipanti = 0;
        conclusori = 0;
        vincitori = 0;
        media = 0;
        is_current = true;
    }

    public Map<String, String> getSolution() {
        return solution;
    }

    @Override
    public String toString(){
        String puzzle = "";
        if(is_current){
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
