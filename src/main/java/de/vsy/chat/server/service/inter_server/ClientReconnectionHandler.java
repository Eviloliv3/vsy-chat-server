package de.vsy.chat.server.service.inter_server;

public
interface ClientReconnectionHandler {

    void processReconnection (final int clientId);
}
