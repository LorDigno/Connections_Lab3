package client;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public class UdpHandler implements Runnable{
    private int port, timeout;
    private Thread game;

    public UdpHandler(int port, int timeout, Thread game){
        this.port = port;
        this.game = game;
        this.timeout = timeout;
    }

    @Override
    public void run() {
        try(DatagramChannel sock = DatagramChannel.open()){
            //faccio il binding alla porta
            InetSocketAddress ad = new InetSocketAddress(port);
            sock.bind(ad);

            String notification = "";
            ByteBuffer buffer = ByteBuffer.allocate(1024);

            while(true){

            }
        }catch (SocketException e){
            System.err.println("Problemi di creazione del socket udp per le notifiche");
            System.exit(1);
        }catch (IOException e){
            System.err.println("Problemi di ricezione udp per le notifiche");
        }
    }
}
