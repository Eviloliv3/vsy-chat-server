package de.vsy.server.data.access;

import de.vsy.server.client_handling.data_management.HandlerLocalDataManager;
import de.vsy.server.client_handling.strategy.PendingClientBufferWatcher;
import de.vsy.shared_module.data_element_validation.IdCheck;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static de.vsy.server.ChatServer.MAX_CLIENT_CONNECTIONS;

public class PendingClientWatcherManager implements PendingClientRegistry {

    private static final long MAX_POOL_SHUTDOWN_SECONDS = 5L;
    private static final Logger LOGGER = LogManager.getLogger();
    private final Map<Integer, Runnable> watchedPendingClients;
    private final ExecutorService pendingWatcherPool;

    public PendingClientWatcherManager() {
        this.watchedPendingClients = new HashMap<>(MAX_CLIENT_CONNECTIONS);
        this.pendingWatcherPool = Executors.newFixedThreadPool(MAX_CLIENT_CONNECTIONS);
    }

    @Override
    public void addPendingClient(final HandlerLocalDataManager clientHandlerData) {
        final var clientId = clientHandlerData.getLocalClientDataProvider().getClientId();
        final var idCheckResult = IdCheck.checkData(clientId);

        if (idCheckResult.isEmpty()) {
            final var pendingBufferWatcher = new PendingClientBufferWatcher(clientHandlerData, this);
            final var existingBufferWatcher = this.watchedPendingClients.putIfAbsent(clientId,
                    pendingBufferWatcher);

            if (existingBufferWatcher == null) {
                this.pendingWatcherPool.execute(pendingBufferWatcher);
            } else {
                LOGGER.warn("Buffer watcher for client {} exists already: {}", clientId,
                        existingBufferWatcher);
            }
        } else {
            LOGGER.error("Client cannot be handled as pending: {}", idCheckResult.get());
        }
    }

    @Override
    public void removePendingClient(final int clientId) {
        this.watchedPendingClients.remove(clientId);
    }

    public void shutdownPendingBufferWatchers() {
        this.watchedPendingClients.clear();
        this.pendingWatcherPool.shutdownNow();
    }

    public void awaitPendingBufferWatchers() throws InterruptedException {
        final var buffersShutdown = this.pendingWatcherPool.awaitTermination(MAX_POOL_SHUTDOWN_SECONDS,
                TimeUnit.SECONDS);

        if (buffersShutdown) {
            LOGGER.info("All pending buffer watchers were shutdown successfully.");
        } else {
            LOGGER.error("Some pending buffer watchers were not shutdown within {} seconds.",
                    MAX_POOL_SHUTDOWN_SECONDS);
        }
    }
}
