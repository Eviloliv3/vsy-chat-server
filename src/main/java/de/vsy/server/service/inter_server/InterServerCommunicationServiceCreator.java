package de.vsy.server.service.inter_server;

public interface InterServerCommunicationServiceCreator {

    /**
     * Creates an InterServerCommunicationService, for an inter server connection.
     */
    void startInterServerCommThread();
}
