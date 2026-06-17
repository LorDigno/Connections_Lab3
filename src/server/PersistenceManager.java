package server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import server.puzzles.RealPuzzle;
import server.puzzles.RealPuzzleFile;
import server.puzzles.UserPuzzle;
import server.users.User;
import server.users.UserFile;
import server.puzzles.UserPuzzleData;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

///Si occupa di salvare i cambiamenti di User, UserPuzzle e RealGame in memoria.
///Flush_all viene chiamato periodicamente.
public class PersistenceManager {
    //uso una AtomicReference per le "cache" che devono essere swappate atomicamente
    private AtomicReference<ConcurrentHashMap<Integer, User>> user_cache;
    private AtomicReference<ConcurrentHashMap<Integer, UserPuzzle>> user_puzzle_cache;
    private AtomicReference<Set<RealPuzzle>> game_cache;

    ///Crea le directory data/, data/users e data/puzzles se non ci sono
    public PersistenceManager() throws FatalServerException{
        //se non esistono le cartelle necessarie le crea
        try {
            Files.createDirectories(Paths.get("data/users"));
            Files.createDirectories(Paths.get("data/puzzles"));
            System.out.println("Directory di persistenza verificate/create con successo.");
        } catch (IOException e) {
            throw new FatalServerException("Impossibile creare le directory di salvataggio.");
        }

        user_cache = new AtomicReference<>(new ConcurrentHashMap<Integer, User>());
        user_puzzle_cache = new AtomicReference<>(new ConcurrentHashMap<Integer, UserPuzzle>());
        game_cache = new AtomicReference<>(ConcurrentHashMap.newKeySet());
    }

    //mark dirty chiamati dagli altri manager
    ///Segna come dirty uno User
    public void mark_user(User u){
        System.out.println("---Marked user " + u);
        user_cache.get().putIfAbsent(u.getId(), u);
    }
    ///Segna come dirty uno UserPuzzle
    public void mark_user_puzzle(UserPuzzle up){
        System.out.println("---Marked user puzzle " + up);
        user_puzzle_cache.get().putIfAbsent(up.user.getId(), up);
    }
    ///Segna come dirty un RealPuzzle
    public void mark_game(RealPuzzle rp){
        System.out.println("---Marked puzzle " + rp);
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
            User u = null;
            UserPuzzle up = null;

            if(id_u.contains(curr)){
                //salvataggio dell'utente users.get(curr)
                u = users.get(curr);
            }

            if(id_p.contains(curr)){
                //salvataggio del puzzle puzzles.get(curr)
                up = puzzles.get(curr);
            }

            write_user_file(curr, u, up);
        }
    }

    private void flush_puzzles(Set<RealPuzzle> to_flush){
        Iterator<RealPuzzle> iter = to_flush.iterator();
        while(iter.hasNext()){
            RealPuzzle curr = iter.next();
            write_puzzle_file(curr);
        }
    }

    ///Chiamato in automatico dallo scheduler per la persistenza periodica.
    ///Fa lo swap delle mappe di cache atomicamente con atomic reference e poi fa il flush dai dati.
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

    ///Fa le modifiche necessarie sul file utente
    private void write_user_file(int id, User user, UserPuzzle puzzle) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        File file = new File("data/users/" + id + ".json");
        UserFile uf;

        //carica esistente o crea nuovo
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                uf = gson.fromJson(reader, UserFile.class);
            } catch (IOException e) {
                System.err.println("Errore lettura file utente, id: " + id);
                //tbd
                return;
            }
        } else {
            uf = new UserFile();
        }

        //aggiorno i dati
        if(user != null){
            uf.fill(user);
        }

        //se c'è sovrascrivo il puzzle
        if(puzzle != null){
            uf.partite.put(new Integer(puzzle.real.id).toString(), new UserPuzzleData(puzzle));
        }

        // scrittura sicura con file temporaneo
        File tmp = new File("data/users/" + id + ".tmp" + Thread.currentThread().getName());
        System.out.println("Scrittura file " + tmp.getPath() + ", " + uf);
        try (FileWriter writer = new FileWriter(tmp)) {
            gson.toJson(uf, writer);
        } catch (IOException e) {
            System.err.println("Errore scrittura file utente, id: " + id);
            return;
        }

        //è atomica su Linux e MacOS
        //uno che leggeva la versione vecchia, continua a farlo (FD con info vecchie)
        //uno che apre subito dopo ha la versione aggiornata
        //tmp.renameTo(file);

        //per test atomico su windows
        try{
            Files.move(tmp.toPath(), file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        }catch (IOException e){
            System.err.println("Errore di copi del file temporaneo");
        }
    }

    ///Fa le modifiche necessarie sul file puzzle
    private void write_puzzle_file(RealPuzzle puzzle){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        File file = new File("data/puzzles/" + puzzle.id + ".json");
        RealPuzzleFile pf;

        //sovrascrivo tutto ogni volta
        pf = new RealPuzzleFile();
        pf.fill(puzzle);

        // scrittura sicura con file temporaneo
        File tmp = new File("data/puzzles/" + puzzle.id + ".tmp" + Thread.currentThread().getName());
        System.out.println("Scrittura file " + tmp.getPath() + "; " + pf);
        try (FileWriter writer = new FileWriter(tmp)) {
            gson.toJson(pf, writer);
        } catch (IOException e) {
            System.err.println("Errore scrittura file partita");
            //tbd
            return;
        }

        //tmp.renameTo(file);
        //per test atomico su windows
        try{
            Files.move(tmp.toPath(), file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        }catch (IOException e){
            System.err.println("Errore di copi del file temporaneo");
        }
    }
}