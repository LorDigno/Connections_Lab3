package server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import server.puzzles.RealPuzzle;
import server.puzzles.UserPuzzle;
import server.users.User;
import server.users.UserFile;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class PersistenceManager {
    //uso una AtomicReference per le "cache" che devono essere swappate atomicamente
    private AtomicReference<ConcurrentHashMap<Integer, User>> user_cache;
    private AtomicReference<ConcurrentHashMap<Integer, UserPuzzle>> user_puzzle_cache;
    private AtomicReference<Set<RealPuzzle>> game_cache;

    public PersistenceManager() {
        user_cache = new AtomicReference<>(new ConcurrentHashMap<Integer, User>());
        user_puzzle_cache = new AtomicReference<>(new ConcurrentHashMap<Integer, UserPuzzle>());
        game_cache = new AtomicReference<>(ConcurrentHashMap.newKeySet());
    }

    //mark dirty chiamati dagli altri manager
    public void mark_user(User u){
        user_cache.get().put(u.getId(), u);
    }
    public void mark_user_puzzle(UserPuzzle up){
        user_puzzle_cache.get().put(up.user.getId(), up);
    }
    public void mark_game(RealPuzzle rp){
        game_cache.get().add(rp);
    }

    //uno per i file utente e uno per i file partita
    private void flush_users(ConcurrentHashMap<Integer, User> users,
                             ConcurrentHashMap<Integer, UserPuzzle> puzzles){
        Set<Integer> id_p = puzzles.keySet();
        Set<Integer> id_u = users.keySet();

        //scorro in base all'id dell'utente così da aprire il file una volta sola
        Set<Integer> id_union = new HashSet<Integer>(id_u);
        id_union.addAll(id_p);

        Iterator<Integer> iter = id_union.iterator();
        while(iter.hasNext()){
            int curr = iter.next();

            //crea il file se non esiste o lo apre se esiste già
            //tbd

            if(id_u.contains(curr)){
                //salvataggio dell'utente users.get(curr)
                //tbd
            }

            if(id_p.contains(curr)){
                //salvataggio del puzzle puzzles.get(curr)
                //tbd
            }
        }
    }

    private void flush_puzzles(Set<RealPuzzle> to_flush){};

    //chiamato in automatico dallo scheduler
    public void flush_all(){
        //il newKeySet della ConcurrentHashMap è thread safe
        //cambio in modo atomico le cache col puntatore così che gli altri thread le possano ririempire
        //  durante il flush di quelle vecchie.
        ConcurrentHashMap<Integer, User> user = user_cache.getAndSet(new ConcurrentHashMap<Integer, User>());
        ConcurrentHashMap<Integer, UserPuzzle> user_puzzle = user_puzzle_cache.getAndSet(new ConcurrentHashMap<Integer, UserPuzzle>());
        Set<RealPuzzle> game = game_cache.getAndSet(ConcurrentHashMap.newKeySet());

        flush_users(user, user_puzzle);
        flush_puzzles(game);
    }


    private void write_user_file(User user, UserPuzzle puzzle) {
        Gson gson = new GsonBuilder().create();
        File file = new File("data/users/" + user.getId() + ".json");
        UserFile uf;

        // carica esistente o crea nuovo
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                uf = gson.fromJson(reader, UserFile.class);
            } catch (IOException e) {
                System.err.println("Errore lettura file utente");
                return;
            }
        } else {
            uf = new UserFile();
        }

        // aggiorna campi base
        uf.id = user.getId();
        uf.username = user.getUsername();
        uf.password = user.getPassword();
        uf.partite_giocate = user.getPartite_giocate();
        uf.partite_vinte = user.getPartite_vinte();
        uf.punteggio_medio = user.getPunteggio_medio();

        // aggiorna o aggiungi UserPuzzle
        if (puzzle != null) {
            boolean found = false;
            for (UserPuzzleData upd : uf.partite) {
                if (upd.game_id == puzzle.getGame_id()) {
                    // aggiorna esistente
                    upd.mistakes = puzzle.mistakes;
                    upd.score = puzzle.score;
                    upd.guesses_left = puzzle.guesses_left;
                    found = true;
                    break;
                }
            }
            if (!found) {
                // aggiungi nuova
                UserPuzzleData upd = new UserPuzzleData();
                upd.game_id = puzzle.getGame_id();
                upd.mistakes = puzzle.mistakes;
                upd.score = puzzle.score;
                upd.guesses_left = puzzle.guesses_left;
                uf.partite.add(upd);
            }
        }

        // scrittura sicura con file temporaneo
        File tmp = new File("data/users/" + user.getId() + ".tmp");
        try (FileWriter writer = new FileWriter(tmp)) {
            gson.toJson(uf, writer);
        } catch (IOException e) {
            System.err.println("Errore scrittura file utente");
            return;
        }
        tmp.renameTo(file);
    }
}