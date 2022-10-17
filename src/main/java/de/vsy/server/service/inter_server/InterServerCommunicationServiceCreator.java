package de.vsy.server.service.inter_server;

import java.net.Socket;

interface InterServerCommunicationServiceCreator {

  void createInterServerService(final boolean isLeader, final Socket remoteServerConnection);
}
