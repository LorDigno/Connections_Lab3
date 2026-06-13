package server.puzzles;

public class RealPuzzleFile {
    int partecipants, winners, finished, id, total_score;

    public void fill(RealPuzzle puzzle){
        synchronized(puzzle){
            this.id = puzzle.id;
            this.partecipants = puzzle.participants;;
            this.winners = puzzle.winners;
            this.finished = puzzle.finished;
            this.total_score = puzzle.total_score;
        }
    }
}
