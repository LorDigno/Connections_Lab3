package server.puzzles;

import server.communication.ResponseStatus;
import server.communication.StatusDescription;
import server.users.User;

import java.util.*;

public class UserPuzzle {
    public RealPuzzle real;
    public User user;
    public List<String> leftover_words;
    public List<List<String>> good_proposals;
    public int mistakes, score, guesses_left, right_ones, real_id;
    private volatile boolean finished;

    //serve per quando carico un file finito che non c'è realpuzzle
    private Map<String, List<String>> real_solution;

    public UserPuzzle(User user, RealPuzzle puzzle){
        this.real = puzzle;
        this.user = user;
        leftover_words = new ArrayList<String>(real.getGroups().keySet());
        mistakes = 0;
        score = 0;
        guesses_left = 4;
        right_ones = 0;
        good_proposals = new ArrayList<List<String>>();
        finished = false;
    }

    public UserPuzzle(UserPuzzleData data){
        this.real_id = data.game_id;
        this.leftover_words = data.leftover_words;
        this.good_proposals = data.good_proposals;
        this.real_solution = data.solution;
        this.mistakes = data.mistakes;
        this.score = data.score;
        this.right_ones = data.right_ones;

        this.real = null;
    }

    public boolean is_finished() {
        return finished;
    }

    private void check_finish(){
        if(!finished){
            if(mistakes > 3 || guesses_left < 1 || right_ones == 3){
                finished = true;
                guesses_left = 0;
            }
        }
    }

    public StatusDescription analyze(List<String> proposal){
        StatusDescription out = new StatusDescription();

        //proposta mal formata
        if(proposal.size() != 4){
            out.setStatus(ResponseStatus.PROPOSAL_WRONG_SIZE);
            out.setDescription("Una proposta deve essere formata da esattamente 4 parole");
            return out;
        }

        Iterator<String> iter = proposal.iterator();
        ArrayList<String> already = new ArrayList<>();
        String group = null;
        while(iter.hasNext()){
            String word = iter.next().toUpperCase();

            //prenso il nome del gruppo corrispondente
            String current_guess = real.getGroups().get(word);

            if(current_guess == null){
                //la parola non è in solution quindi è sconosciuta
                out.setStatus(ResponseStatus.PROPOSAL_UNKNOWN_WORD);
                out.setDescription("La parola \""+word+"\" non fa parte del puzzle");
                return out;
            }
            if(!leftover_words.contains(word)){
                //la parola non è ancora in gioco
                out.setStatus(ResponseStatus.PROPOSAL_ALREADY_GROUPED);
                out.setDescription("La parola \""+word+"\" è già stata raggruppata correttamente");
                return out;
            }
            if(already.contains(word)){
                //la parola è più volte nella proposta, il client non lo permette ma non si sa mai
                out.setStatus(ResponseStatus.PROPOSAL_REPEATED_WORD);
                out.setDescription("La parola \""+word+"\" è presente più volta nella proposta");
                return out;
            }
            already.add(word);

            //mi salvo la guess possibile alla prima
            if(group == null){
                group = current_guess;
            }

            if(!current_guess.equals(group)){
                //una delle parole ha corrispondenza diversa quindi la proposta è fallimentare
                synchronized(this){
                    guesses_left += -1;
                    score += -4;
                    mistakes += 1;

                    check_finish();
                }

                out.setStatus(ResponseStatus.OK);
                out.setDescription("La proposta contiene almeno una parola non connessa alle altre" +
                        "\nSottratti 4 punti al tuo punteggio" +
                        "\nTentativi rimanenti: " + (guesses_left));

                return out;
            }
        }

        synchronized(this){
            guesses_left += -1;
            score += 6;
            right_ones += 1;
            good_proposals.add(proposal);

            check_finish();

            //rimozione delle parole indovinate da quelle rimanenti
            iter = proposal.iterator();
            while(iter.hasNext()){
                leftover_words.remove(iter.next());
            }
        }


        out.setStatus(ResponseStatus.OK);
        out.setDescription("La proposta: " + proposal.toString() + "è corretta!!!" +
                "\nIl tema era: \"" + group + "\"" +
                "\n Aggiunnti 6 punti al tuo punteggio" +
                "\nTentativi rimanenti: " + (guesses_left));

        return out;
    }

    public synchronized String get_puzzle_state() {
        String puzzle = "";
        puzzle += "Proposte Corrette: " + good_proposals.toString() + "\n";
        puzzle += "Parole Rimaste: " + leftover_words.toString() + "\n";
        puzzle += "Errori Commessi: " + mistakes + "\n";
        puzzle += "Punteggio: " + score + "\n";
        return puzzle;
    }

    public synchronized String get_puzzle_stats() {
        String puzzle = "";
        if(real == null){
            puzzle += "Gruppi Corretti: " + real_solution.toString() + "\n";
        }else{
            puzzle += "Gruppi Corretti: " + real.solution.toString() + "\n";
        }
        puzzle += "Proposte Corrette: " + right_ones + "\n";
        puzzle += "Errori Commessi: " + mistakes + "\n";
        puzzle += "Punteggio: " + score + "\n";
        return puzzle;
    }

}
