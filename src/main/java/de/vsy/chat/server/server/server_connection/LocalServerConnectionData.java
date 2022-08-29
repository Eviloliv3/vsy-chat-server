package de.vsy.chat.server.server.server_connection;

import java.io.IOException;
import java.net.ServerSocket;

/** Enhält verbindungsbezogene Informationen der lokalen Serverinstanz. */
public
class LocalServerConnectionData implements ServerConnectionDataProvider {

    private final int serverId;
    private final ServerSocket serverSocket;

    /**
     * Instantiates a new server connection dataManagement.
     *
     * @param socket the socket
     */
    private
    LocalServerConnectionData (final int serverId, final ServerSocket socket) {
        this.serverId = serverId;
        this.serverSocket = socket;
    }

    public static
    LocalServerConnectionData valueOf (final int serverId,
                                       final ServerSocket serverSocket) {
        if (serverSocket == null) {
            throw new NullPointerException("Kein ServerSocket angeben.");
        }
        return new LocalServerConnectionData(serverId, serverSocket);
    }

    public
    ServerSocket getConnectionSocket () {
        return this.serverSocket;
    }

    @Override
    public
    String getHostname () {
        return this.serverSocket.getInetAddress().getHostName();
    }

    @Override
    public
    int getServerPort () {
        return this.serverSocket.getLocalPort();
    }

    @Override
    public
    int getServerId () {
        return serverId;
    }

    @Override
    public
    boolean closeConnection ()
    throws IOException {
        this.serverSocket.close();
        return this.serverSocket.isClosed();
    }
}
