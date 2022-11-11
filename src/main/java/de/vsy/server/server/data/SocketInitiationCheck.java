package de.vsy.server.server.data;

public interface SocketInitiationCheck {

  void waitForUninitiatedConnections() throws InterruptedException;
}
