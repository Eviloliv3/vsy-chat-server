package de.vsy.server.data;

public interface SocketInitiationCheck {

    void waitForUninitiatedConnections() throws InterruptedException;
}
