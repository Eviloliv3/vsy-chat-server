package de.vsy.server.server_connection;


import de.vsy.server.data.socketConnection.LocalServerConnectionData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.Socket;

public class ClientConnectionEstablisher {

    private static final Logger LOGGER = LogManager.getLogger();
    private final ClientServer server;
    private final LocalServerConnectionData clientConnectionData;

    public ClientConnectionEstablisher(final LocalServerConnectionData localClientConnectionData,
                                       final ClientServer server) {
        this.clientConnectionData = localClientConnectionData;
        this.server = server;
    }

    public void acceptClientConnections() {
        final var clientConnectionAcceptor = clientConnectionData.getConnectionSocket();
        Socket clientConnectionSocket;

        if (clientConnectionAcceptor != null) {

            while (!(clientConnectionAcceptor.isClosed())) {

                try {
                    clientConnectionSocket = clientConnectionAcceptor.accept();

                    if (clientConnectionSocket != null) {
                        this.server.serveClient(clientConnectionSocket);
                    }
                } catch (IOException ioe) {
                    LOGGER.error("{} occurred while waiting for a new client connection -> {}",
                            ioe.getClass().getSimpleName(), ioe.getMessage());
                    break;
                }
            }
            LOGGER.info("No new client connections will be accepted.");
        } else {
            LOGGER.error("No ServerSocket has been provided.");
        }
    }

    public void stopEstablishingConnections() {
        LOGGER.info("Stopping client connection acceptance.");
        try {
            clientConnectionData.closeConnection();
        } catch (IOException ioe) {
            LOGGER.error("Client connection socket closing attempt failed: {} - {}",
                    ioe.getClass().getSimpleName(), ioe.getMessage());
        }
        LOGGER.info("No more client connections will be accepted.");
    }
}
