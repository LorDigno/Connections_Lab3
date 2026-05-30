package server.puzzles;


import java.util.Map;

public class RealPuzzle {
    public int id;
    private final Map<String, String> solution;

    public RealPuzzle(int id, Map<String,String> groups){
        this.id = id;
        solution = groups;
    }

    public Map<String, String> getSolution() {
        return solution;
    }
}
