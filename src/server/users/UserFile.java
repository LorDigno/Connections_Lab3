package server.users;

import server.puzzles.UserPuzzle;
import java.util.HashMap;

// classe che rappresenta il file utente
public class UserFile {
    int id;
    String username;
    String password;
    int completed;
    int wins;
    int losses;
    int incomplete;
    float win_rate;
    float loss_rate;
    int curr_streak;
    int max_streak;
    int perfects;
    int mistake1, mistake2, mistake3;
    HashMap<Integer, UserPuzzleData> partite = new HashMap<Integer, UserPuzzleData>();
}
