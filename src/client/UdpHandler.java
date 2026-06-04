package client;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

public class UdpHandler implements Runnable{
    private DatagramChannel udp_sock;
    private AtomicBoolean interrupt;
    private Thread game;



    public UdpHandler(DatagramChannel udp_sock, AtomicBoolean interrupt, Thread game){
        this.udp_sock = udp_sock;
        this.interrupt = interrupt;
        this.game = game;
    }

    @Override
    public void run(){
        //alloco il bytebuffer
        ByteBuffer buffer = ByteBuffer.allocate(4096);

        try {
            while (true) {
                //preparo la scrittura
                buffer.clear();

                udp_sock.receive(buffer);

                //preparo la lettura
                buffer.flip();

                // Decodifica direttamente il buffer in una stringa
                String messaggio = StandardCharsets.US_ASCII.decode(buffer).toString();
                System.out.println(messaggio + "\n***Operazioni in sospeso annullate");

                interrupt.set(true);
                game.interrupt();
            }
        }catch(AsynchronousCloseException e) {
            //il client ha fatto il logout o il reset
        }catch (IOException e){
            System.err.println("Errore di IO alla recezione di una notifica");
        }

    }
}
