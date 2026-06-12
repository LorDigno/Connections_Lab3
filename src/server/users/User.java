package server.users;

import java.util.concurrent.atomic.AtomicInteger;

public class User {
    //dati dell'account
    protected static AtomicInteger next_id = new AtomicInteger(0);
    private int id;
    private String username, password;

    //statistiche dell'utente
    public int played, curr_streak, max_streak, perfect, one_mistake, two_mistakes,
            three_mistakes, failed, incomplete, wins;
    public float win_rate, loss_rate;

    public User(String username, String password){
        this.username = username;
        this.password = password;

        id = User.next_id.getAndIncrement();
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
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getId(){
        return this.id;
    }
}
