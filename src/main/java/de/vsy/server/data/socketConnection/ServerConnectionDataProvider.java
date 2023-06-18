package de.vsy.server.data.socketConnection;

import java.io.IOException;

public interface ServerConnectionDataProvider {

    /**
     * Returns the server's hostname
     *
     * @return local hostname as String
     */
    String getHostname();

    /**
     * Returns the port associated with this socket connection.
     *
     * @return local port as int
     */
    int getServerPort();

    /**
     * Returns the server id (usually the server port)
     *
     * @return local OR remote server id; depending on implementing class
     */
    int getServerId();

    boolean closeConnection() throws IOException;
}
