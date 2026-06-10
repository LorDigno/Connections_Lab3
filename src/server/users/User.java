package server.users;

public class User {
    //dati dell'account
    private static int next_id = 0;
    private int id;
    private String username, password;

    //statistiche di gioco
    private int completed, curr_streak, max_streak, perfect, one_mistake, two_mistakes,
            three_mistakes, failed, incomplete;
    private float win_rate, loss_rate;

    public User(String username, String password){
        this.username = username;
        this.password = password;

        id = User.next_id;
        User.next_id += 1;
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
}
