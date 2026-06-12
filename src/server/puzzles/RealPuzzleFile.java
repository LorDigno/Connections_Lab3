package server.puzzles;

public class RealPuzzleFile {
    int time_left;
    int partecipants, winners, finished;
    float avg_score;

    public void fill(RealPuzzle puzzle){
        this.time_left = puzzle.time_left;
        this.partecipants = puzzle.partecipants;;
        this.winners = puzzle.winners;
        this.finished = puzzle.finished;
        this.avg_score = puzzle.avg_score;
    }
}
