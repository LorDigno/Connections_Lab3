package server.users;


import java.net.InetAddress;

public class UserSession {
    public final String username;
    public final InetAddress address;
    public final int udp_port;

    public UserSession(String username, InetAddress address, int udp_port) {
        this.username = username;
        this.address = address;
        this.udp_port = udp_port;
    }
}

