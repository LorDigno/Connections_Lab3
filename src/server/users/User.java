package server.users;

import java.util.concurrent.atomic.AtomicInteger;

///Rappresenta un utente, contiene tutti i suoi dati
///Molti metodi sono synchronized perché non si vuole l'atomicità sul singolo campo ma sull'intero stato dell'utente
public class User {
    //dati dell'account
    protected static AtomicInteger next_id = new AtomicInteger(0);
    private final int id;
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

    ///Cambia username e password se gli input non sono null
    public synchronized void update_credentials(String new_u, String new_p){
        if(new_u != null){
            username = new_u;
        }
        if(new_p != null){
            password = new_p;
        }
    }

    ///Non sincronizzato perché id è readonly e final
    public int getId(){
        return this.id;
    }

    public synchronized int get_score(){
        return this.total_score;
    }

    ///Al login si aggiunge una partita giocata e una incompleta
    public synchronized void add_game(){
        played += 1;
        incomplete += 1;
    }

    ///Rende il payload per requestPlayerStats
    public String get_stats(){
        //faccio lo snapshot dello stato per non bloccarmi mentre creo la stringa
        int p, per, m1, m2, m3, m4, i, w, l, cs, ms;
        synchronized(this){
            p = played;
            per = perfect;
            m1 = one_mistake;
            m2 = two_mistakes;
            m3 = three_mistakes;
            m4 = four_mistakes;
            i = incomplete;
            w = wins;
            l = failed;
            cs = curr_streak;
            ms = max_streak;
        }


        String stats = "";
        stats += "Puzzle giocati: " + p + "\n";
        stats += "Puzzle perfetti (vinti con 0 errori): " + per + "\n";
        stats += "Puzzle con 1 errore: " + m1 + "\n";
        stats += "Puzzle con 2 errori: " + m2 + "\n";
        stats += "Puzzle con 3 errori: " + m3 + "\n";
        stats += "Puzzle con 4 errori: " + m4 + "\n";
        stats += "Puzzle non completati: " + i + "\n";

        if(p == 0){
            stats += "WinRate: " + 0 + "%\n";
            stats += "LossRate: " + 0 + "%\n";
        }else{
            stats += "WinRate: " + (float) w/p + "%\n";
            stats += "LossRate: " + (float) l/p + "%\n";
        }

        stats += "Filone attuale: " + cs + " partite vinte di fila\n";
        stats += "Filone migliore: " + ms + " partite vinte di fila\n";

        return stats;
    }

    public String toString(){
        return username + ";" + id + ";" + played + ";" + get_score();
    }
}
