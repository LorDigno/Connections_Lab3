package server.users;

import java.util.concurrent.atomic.AtomicInteger;

public class User {
    //dati dell'account
    protected static AtomicInteger next_id = new AtomicInteger(0);
    private int id;
    private String username, password;

    //statistiche dell'utente
    protected int played, curr_streak, max_streak, perfect, one_mistake, two_mistakes,
            three_mistakes, four_mistakes,failed, incomplete, wins, total_score;
    public float win_rate, loss_rate;

    public User(String username, String password){
        this.username = username;
        this.password = password;

        id = User.next_id.getAndIncrement();

        this.played = 0;
        this.wins = 0;
        this.failed = 0;
        this.incomplete = 0;
        this.win_rate = 0;
        this.loss_rate = 0;
        this.curr_streak = 0;
        this.max_streak = 0;
        this.perfect = 0;
        this.one_mistake = 0;
        this.two_mistakes = 0;
        this.three_mistakes = 0;
        this.four_mistakes = 0;
        this.total_score = 0;
    }
    
    public User(UserFile uf){
        this.id = uf.id;
        this.username = uf.username;
        this.password = uf.password;
        this.played = uf.played;
        this.wins = uf.wins;
        this.failed = uf.failed;
        this.incomplete = uf.incomplete;
        this.win_rate = uf.win_rate;
        this.loss_rate = uf.loss_rate;
        this.curr_streak = uf.curr_streak;
        this.max_streak = uf.max_streak;
        this.perfect = uf.perfects;
        this.one_mistake = uf.mistake1;
        this.two_mistakes = uf.mistake2;
        this.three_mistakes = uf.mistake3;
        this.four_mistakes = uf.mistake4;
        this.total_score = uf.total_score;
    }

    public synchronized String getPassword() {
        return password;
    }

    public synchronized String getUsername() {
        return username;
    }

    public synchronized void update_credentials(String new_u, String new_p){
        if(new_u != null){
            username = new_u;
        }
        if(new_p != null){
            password = new_p;
        }
    }

    public int getId(){
        return this.id;
    }

    public synchronized int get_score(){
        return this.total_score;
    }

    //al login si aggiunge una partita giocata e una incompleta
    public synchronized void add_game(){
        played += 1;
        incomplete += 1;
    }

    public synchronized String get_stats(){
        String stats = "";
        stats += "Puzzle giocati: " + played + "\n";
        stats += "Puzzle perfetti (vinti con 0 errori): " + perfect + "\n";
        stats += "Puzzle con 1 errore: " + one_mistake + "\n";
        stats += "Puzzle con 2 errori: " + two_mistakes + "\n";
        stats += "Puzzle con 3 errori: " + three_mistakes + "\n";
        stats += "Puzzle con 4 errori: " + four_mistakes + "\n";
        stats += "Puzzle non completati: " + incomplete + "\n";

        if(played == 0){
            stats += "WinRate: " + 0 + "%\n";
            stats += "LossRate: " + 0 + "%\n";
        }else{
            stats += "WinRate: " + (float) wins/played + "%\n";
            stats += "LossRate: " + (float) failed/played + "%\n";
        }

        stats += "Filone attuale: " + curr_streak + " partite vinte di fila\n";
        stats += "Filone migliore: " + max_streak + " partite vinte di fila\n";

        return stats;
    }
}
