/*
 *
 */
package de.vsy.server.server.data;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.List.copyOf;

public
class ConnectionSpecifications {

    private static final String HOSTNAME = "127.0.0.1";
    /**
     * Die zwei maximal zu verwendenden Serverports; dienen gleichzeitig zur
     * Identifikation des Servers im "Servernetz".
     */
    private static final List<Integer> SERVER_PORTS = new ArrayList<>(
            asList(7370, 7371));
    /** Der einzelne Port zur Verbindung zwischen zwei Servern. */
    private static final int TRANS_SERVER_PORT = 8000;

    private
    ConnectionSpecifications () {
    }

    /**
     * Gets the HOSTNAME.
     *
     * @return the HOSTNAME
     */
    public static
    String getHostname () {
        return HOSTNAME;
    }

    /**
     * Gets the serverports.
     *
     * @return the serverports
     */
    public static
    List<Integer> getServerports () {
        return copyOf(SERVER_PORTS);
    }

    /**
     * Gets the transserverport.
     *
     * @return the transserverport
     */
    public static
    int getTransserverport () {
        return TRANS_SERVER_PORT;
    }
}
