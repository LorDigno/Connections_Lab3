package server.puzzles;

import server.Status;
import server.StatusDescription;
import server.users.User;

import java.util.*;

public class UserPuzzle {
    public transient RealPuzzle real;
    public transient User user;
    private List<String> leftover_words;
    private List<List<String>> good_proposals;
    public int mistakes, score, guesses_left;
    //time_left in millisecondi

    public UserPuzzle(User user, RealPuzzle puzzle){
        this.real = puzzle;
        this.user = user;
        leftover_words = new ArrayList<String>(real.getGroups().keySet());
        mistakes = 0;
        score = 0;
        guesses_left = 4;
        good_proposals = new ArrayList<List<String>>();
    }

    public StatusDescription analyze(List<String> proposal){
        StatusDescription out = new StatusDescription();

        //proposta mal formata
        if(proposal.size() != 4){
            out.setStatus(Status.PROPOSAL_WRONG_SIZE);
            out.setDescription("Una proposta deve essere formata da esattamente 4 parole");
            return out;
        }

        Iterator<String> iter = proposal.iterator();
        ArrayList<String> already = new ArrayList<>();
        String group = null;
        while(iter.hasNext()){
            String word = iter.next();

            //prenso il nome del gruppo corrispondente
            String current_guess = real.getGroups().get(word);

            if(current_guess == null){
                //la parola non è in solution quindi è sconosciuta
                out.setStatus(Status.PROPOSAL_UNKNOWN_WORD);
                out.setDescription("La parola \""+word+"\" non fa parte del puzzle");
                return out;
            }
            if(!leftover_words.contains(word)){
                //la parola non è ancora in gioco
                out.setStatus(Status.PROPOSAL_ALREADY_GROUPED);
                out.setDescription("La parola \""+word+"\" è già stata raggruppata correttamente");
                return out;
            }
            if(already.contains(word)){
                //la parola è più volte nella proposta, il client non lo permette ma non si sa mai
                out.setStatus(Status.PROPOSAL_REPEATED_WORD);
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
                guesses_left += -1;

                out.setStatus(Status.OK);
                out.setDescription("La proposta contiene almeno una parola non connessa alle altre" +
                        "\nSottratti 4 punti al tuo punteggio" +
                        "\nTentativi rimanenti: " + (guesses_left));

                score += -4;
                mistakes += 1;
                return out;
            }
        }

        guesses_left += -1;

        out.setStatus(Status.OK);
        out.setDescription("La proposta: " + proposal.toString() + "è corretta!!!" +
                "\nIl tema era: \"" + group + "\"" +
                "\n Aggiunnti 6 punti al tuo punteggio" +
                "\nTentativi rimanenti: " + (guesses_left));

        score += 6;
        good_proposals.add(proposal);

        //rimozione delle parole indovinate da quelle rimanenti
        iter = proposal.iterator();
        while(iter.hasNext()){
            leftover_words.remove(iter.next());
        }

        return out;
    }

    public String get_puzzle_state() {
        String puzzle = "";
        puzzle += "Tempo Rimasto: " + real.time_left + "ms\n";
        puzzle += "Proposte Corrette: " + good_proposals.toString() + "\n";
        puzzle += "Parole Rimaste: " + leftover_words.toString() + "\n";
        puzzle += "Errori Commessi: " + mistakes + "\n";
        puzzle += "Punteggio: " + score + "\n";
        return puzzle;
    }

    public String get_puzzle_stats() {
        String puzzle = "";
        puzzle += "Gruppi Corretti: " + real.solution.toString() + "\n";
        int corrects = 4 - mistakes - guesses_left;
        puzzle += "Proposte Corrette: " + corrects + "\n";
        puzzle += "Errori Commessi: " + mistakes + "\n";
        puzzle += "Punteggio: " + score + "\n";
        return puzzle;
    }

}
