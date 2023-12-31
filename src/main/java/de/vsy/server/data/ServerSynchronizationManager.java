package de.vsy.server.data;

import java.util.concurrent.CountDownLatch;

public class ServerSynchronizationManager {

    private final CountDownLatch clientSynchronization;
    private final SocketInitiationCheck uninitiatedServers;

    public ServerSynchronizationManager(final SocketInitiationCheck uninitiatedServers) {
        this.clientSynchronization = new CountDownLatch(1);
        this.uninitiatedServers = uninitiatedServers;
    }

    public void waitForClientSynchronization() throws InterruptedException {
        this.clientSynchronization.await();
    }

    public void clientSynchronizationComplete() {
        this.clientSynchronization.countDown();
    }

    public void waitForUninitiatedSocketConnections() throws InterruptedException {
        this.uninitiatedServers.waitForUninitiatedConnections();
    }
}
