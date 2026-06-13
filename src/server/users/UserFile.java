package server.users;

import server.puzzles.UserPuzzleData;

import java.util.HashMap;
import java.util.Map;

// classe che rappresenta il file utente
public class UserFile {
    int id;
    String username;
    String password;
    int played;
    int wins;
    int failed;
    int incomplete;
    float win_rate;
    float loss_rate;
    int curr_streak;
    int max_streak;
    int perfects;
    int mistake1, mistake2, mistake3, mistake4;
    public Map<Integer, UserPuzzleData> partite;

    public UserFile(){
        this.partite = new HashMap<Integer, UserPuzzleData>();
    }

    public void fill(User user){
        //garantisco di leggere tutti i dati coerenti ad un singolo stato
        synchronized(user){
            this.id = user.getId();
            this.username = user.getUsername();
            this.password = user.getPassword();
            this.played = user.played;
            this.wins = user.wins;
            this.failed = user.failed;
            this.incomplete = user.incomplete;
            this.win_rate = user.win_rate;
            this.loss_rate = user.loss_rate;
            this.curr_streak = user.curr_streak;
            this.max_streak = user.max_streak;
            this.perfects = user.perfect;
            this.mistake1 = user.one_mistake;
            this.mistake2 = user.two_mistakes;
            this.mistake3 = user.three_mistakes;
            this.mistake4 = user.four_mistakes;
        }

    }
}
