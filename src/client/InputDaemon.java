package client;

import java.util.Scanner;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;


///Prende gli input da stdin e poi li manda al main_thread tramite una blockingqueue
public class InputDaemon implements Runnable{

    private BlockingQueue<String> queue;
    private AtomicBoolean reject;
    public InputDaemon (BlockingQueue<String> queue, AtomicBoolean reject){
        this.queue = queue;
        this.reject = reject;
    }

    public void run(){
        String input = "";
        Scanner scanner = new Scanner(System.in);

        while(true){
            if (scanner.hasNextLine()) {
                input = scanner.nextLine().strip().toLowerCase();
            }
            if(reject.get()){
                System.out.println("---Input: \""+ input + "\" respinto");
            }
            else{
                boolean b = queue.offer(input);
                if(!b){
                    System.out.println("---Errore di lettura dell'input: \""+ input);
                }
            }
        }

    }
}
