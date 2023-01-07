package de.vsy.server.data.socketConnection;

import java.io.IOException;

public interface ServerConnectionDataProvider {

    /**
     * Returns the hostname
     *
     * @return local hostname as String
     */
    String getHostname();

    /**
     * Returns the server port
     *
     * @return local port as int
     */
    int getServerPort();

    /**
     * Returns the server id
     *
     * @return local OR remote server id; depending on implementing class
     */
    int getServerId();

    boolean closeConnection() throws IOException;
}
