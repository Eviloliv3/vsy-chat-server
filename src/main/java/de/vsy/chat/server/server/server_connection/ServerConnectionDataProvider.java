package de.vsy.chat.server.server.server_connection;

import java.io.IOException;

public
interface ServerConnectionDataProvider {

    /**
     * Gets the hostname
     *
     * @return local hostname as String
     */
    String getHostname ();

    /**
     * Gets the server port
     *
     * @return local port as int
     */
    int getServerPort ();

    /**
     * Gets the server id
     *
     * @return local OR remote server id; depending on implementing class
     */
    int getServerId ();

    boolean closeConnection ()
    throws IOException;
}
