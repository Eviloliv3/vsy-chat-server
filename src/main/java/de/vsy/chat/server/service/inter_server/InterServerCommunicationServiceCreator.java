package de.vsy.chat.server.service.inter_server;

import java.net.Socket;

interface InterServerCommunicationServiceCreator {

    void createInterServerService (final boolean isLeader,
                                   final Socket remoteServerConnection);
}
