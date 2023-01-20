
package de.vsy.server.data;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.List.copyOf;

public class ConnectionSpecifications {

    private static final String HOSTNAME = "127.0.0.1";
    private static final List<Integer> SERVER_PORTS = new ArrayList<>(asList(7370, 7371));
    private static final int INTER_SERVER_PORT = 8000;

    private ConnectionSpecifications() {
    }

    /**
     * Returns the HOSTNAME.
     *
     * @return the HOSTNAME
     */
    public static String getHostname() {
        return HOSTNAME;
    }

    /**
     * Returns the server ports.
     *
     * @return List<Integer>
     */
    public static List<Integer> getServerPorts() {
        return copyOf(SERVER_PORTS);
    }

    /**
     * Returns the server port.
     *
     * @return int
     */
    public static int getInterServerPort() {
        return INTER_SERVER_PORT;
    }
}
