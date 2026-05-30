package server.puzzles;

import java.util.*;

public class UserPuzzle {
    public int id;
    public String username;
    private int mistakes, corrects, score;
    Map<String, List<String>> solution;

    public UserPuzzle(CurrentPuzzle curr, String username){
        this.id = curr.real.id;
        this.username = username;
        this.mistakes = curr.mistakes;
        this.corrects =  4 - curr.guesses_left - mistakes;
        this.score = curr.score;

        //inverto solutions da parola -> gruppo a gruppo -> parola
        this.solution = new HashMap<String, List<String>>();
        Iterator<String> keys = curr.real.getSolution().keySet().iterator();
        while(keys.hasNext()){
            String word = keys.next();
            String name = curr.real.getSolution().get(word);

            if(!solution.containsKey(name)){
                solution.put(name, new ArrayList<String>());
            }

            solution.get(name).add(word);
        }
    }

    @Override
    public String toString() {
        String puzzle = "";
        puzzle += "Gruppi Corretti: " + solution.toString() + "\n";
        puzzle += "Proposte Corrette: " + corrects + "\n";
        puzzle += "Errori Commessi: " + mistakes + "\n";
        puzzle += "Punteggio: " + score + "\n";
        return puzzle;
    }
}
