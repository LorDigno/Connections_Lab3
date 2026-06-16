package server.puzzles;

import java.util.List;
import java.util.Map;

public class UserPuzzleData{
    int game_id;
    List<String> leftover_words;
    List<List<String>> good_proposals;
    Map<String, List<String>> solution;
    int mistakes;
    int score;
    int right_ones;

    public UserPuzzleData(UserPuzzle puzzle){
        this.game_id = puzzle.real.id;
        this.leftover_words = puzzle.leftover_words;
        this.good_proposals = puzzle.good_proposals;
        this.solution = puzzle.real.solution;
        this.mistakes = puzzle.mistakes;;
        this.score = puzzle.score;
        this.right_ones = 4 - puzzle.guesses_left - mistakes;
    }
}
