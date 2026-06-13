package client.operations;

import client.ClientJsonUtils;
import client.GameClient;
import client.UserStatus;

import java.util.ArrayList;
import java.util.List;

public class SubmitProposalOp extends Operation{

    public SubmitProposalOp(GameClient game){
        this.game = game;
        this.name = "submitProposal";
    }

    @Override
    public boolean checks() {
        if(game.u_status != UserStatus.LOGGED_IN){
            System.out.println("---\tDevi aver fatto un accesso prima di poter giocare");
            return false;
        }

        return true;
    }

    @Override
    public String payload() throws InterruptedException{
        //chiedo all'utente le parole da proporre
        System.out.print("Inserisci le parole che compongono la tua proposta\n");

        //prendo le 4 parole
        int conto = 0;
        List<String> words = new ArrayList<String>();
        while(conto < 4){
            String parola = get_string("Parola " + (conto + 1) + ": ");
            if(words.contains(parola)){
                System.out.print("---Ogni parola deve essere unica\n");
                continue;
            }

            words.add(parola);
            conto += 1;
        }

        //creo la jsonstring
        return ClientJsonUtils.get_submitProposal_message(game.puzzle_id.get() ,words);
    }

    @Override
    public void on_fail(){}

    @Override
    public void digest(String response) {
        int response_status = ClientJsonUtils.get_int(response, "status", name);
        String desc = ClientJsonUtils.get_string(response, "description",name);

        switch(response_status){
            case 0:
                System.out.println(desc);
                break;

            case -1:
                System.out.println("Errore di comunicazione durante l'invio della proposta");
                break;

            default:
                //comunico all'utente l'errore
                System.out.println("Errore [" + response_status +"]\n\t" + desc);
        }
    }
}
