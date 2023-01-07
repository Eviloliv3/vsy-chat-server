package de.vsy.server.server_connection;

import java.net.Socket;

public interface ClientServer {

    void serveClient(Socket clientConnection);

    void shutdownServer();
}
