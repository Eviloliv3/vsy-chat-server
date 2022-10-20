package de.vsy.server.server;

import java.net.Socket;

public interface ClientServer {

  void serveClient(Socket clientConnection);

  void shutdownServer();
}
