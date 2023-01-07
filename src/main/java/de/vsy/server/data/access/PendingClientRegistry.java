package de.vsy.server.data.access;

import de.vsy.server.client_handling.data_management.HandlerLocalDataManager;

public interface PendingClientRegistry {
    void addPendingClient(final HandlerLocalDataManager clientHandlerData);
    void removePendingClient(final int clientId);
}
