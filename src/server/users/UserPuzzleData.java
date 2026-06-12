package server.users;

import java.util.HashMap;
import java.util.List;

class UserPuzzleData {
    int game_id;
    List<String> leftover_words;
    List<List<String>> good_proposals;
    HashMap<String, List<String>> groups;
    int mistakes;
    int score;
    int right_ones;
}
