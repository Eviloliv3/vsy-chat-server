package de.vsy.server.service.inter_server;

public interface ClientReconnectionHandler {

    /**
     * Handles the reinstallation of a ClientConnectionHandler for a specific
     * reconnected client.
     *
     * @param clientId the client's id
     */
    void processReconnection(final int clientId);

    void stopReconnectingClients();
}
