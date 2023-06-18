package de.vsy.server.data;

@FunctionalInterface
public interface SocketInitiationCheck {

    /**
     * Lets threads wait for uninitiated server connections to be properly
     * established/synchronized.
     *
     * @throws InterruptedException if thread was interrupted while waiting
     */
    void waitForUninitiatedConnections() throws InterruptedException;
}
