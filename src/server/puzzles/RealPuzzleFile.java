package server.puzzles;

public class RealPuzzleFile {
    int time_left;
    int partecipants, winners, finished;
    float avg_score;

    public void fill(RealPuzzle puzzle){
        synchronized(puzzle){
            this.partecipants = puzzle.partecipants;;
            this.winners = puzzle.winners;
            this.finished = puzzle.finished;
            if(puzzle.partecipants != 0){
                this.avg_score = (float) puzzle.total_score / puzzle.partecipants;
            }else{
                this.avg_score = 0;
            }
        }
    }
}
