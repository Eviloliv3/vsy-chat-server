package de.vsy.server.server_connection;

import java.net.Socket;

/**
 * Used for server testing.
 */
public interface ClientServer {

    void serveClient(Socket clientConnection);

    void shutdownServer();
}
