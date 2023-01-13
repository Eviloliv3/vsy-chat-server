package de.vsy.server.data.access;

import de.vsy.server.client_handling.data_management.HandlerLocalDataManager;

/**
 * Provides access to server's centralized pending client management.
 */
public interface PendingClientRegistry {
    /**
     * Add client to pending client management, providing client's data access.
     *
     * @param clientHandlerData the pending client's data manager
     */
    void addPendingClient(final HandlerLocalDataManager clientHandlerData);

    /**
     * Remove client from pending client management, using his id for identification.
     *
     * @param clientId the client's id
     */
    void removePendingClient(final int clientId);
}
