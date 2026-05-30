package server.puzzles;

import server.StatusDescription;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CurrentPuzzle{
    public RealPuzzle real;
    private List<String> leftover_words;
    private List<List<String>> good_proposals;
    public int mistakes, score, time_left, guesses_left;
    //time_left in millisecondi

    public CurrentPuzzle(RealPuzzle puzzle, List<String> words){
        this.real = puzzle;
        leftover_words = words;
        this.time_left = real.time_left;
        mistakes = 0;
        score = 0;
        guesses_left = 4;
        good_proposals = new ArrayList<List<String>>();
    }

    public StatusDescription analyze(List<String> proposal){
        StatusDescription out = new StatusDescription();

        //proposta mal formata
        if(proposal.size() != 4){
            out.setStatus(3);
            out.setDescription("Una proposta deve essere formata da esattamente 4 parole");
            return out;
        }

        Iterator<String> iter = proposal.iterator();
        String group = null;
        while(iter.hasNext()){
            String word = iter.next();

            //prenso il nome del gruppo corrispondente
            String current_guess = real.getSolution().get(word);

            if(current_guess == null){
                //la parola non è inn solution quindi è sconosciuta
                out.setStatus(1);
                out.setDescription("La parola \""+word+"\" non fa parte del puzzle");
                return out;
            }
            if(!leftover_words.contains(word)){
                //la parola non è ancora in gioco
                out.setStatus(2);
                out.setDescription("La parola \""+word+"\" è già stata raggruppata correttamente");
                return out;
            }

            //mi salvo la guess possibile alla prima
            if(group == null){
                group = current_guess;
            }

            if(!current_guess.equals(group)){
                //una delle parole ha corrispondenza diversa quindi la proposta è fallimentare
                guesses_left += -1;

                out.setStatus(0);
                out.setDescription("La proposta contiene almeno una parola non connessa alle altre" +
                        "\nSottratti 4 punti al tuo punteggio" +
                        "\nTentativi rimanenti: " + (guesses_left));

                score += -4;
                mistakes += 1;
                return out;
            }
        }

        guesses_left += -1;

        out.setStatus(0);
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

    public String

    @Override
    public String toString() {
        String puzzle = "";
        puzzle += "Tempo Rimasto: " + time_left + "ms\n";
        puzzle += "Proposte Corrette: " + good_proposals.toString() + "\n";
        puzzle += "Parole Rimaste: " + leftover_words.toString() + "\n";
        puzzle += "Errori Commessi: " + mistakes + "\n";
        puzzle += "Punteggio: " + score + "\n";
        return puzzle;
    }

}
