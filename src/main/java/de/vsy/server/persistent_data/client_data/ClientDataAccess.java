package de.vsy.server.persistent_data.client_data;

import de.vsy.server.persistent_data.PersistentDataAccess;

/**
 * Provides means for client data access setup during runtime.
 */
public interface ClientDataAccess extends PersistentDataAccess {

    /**
     * Creates the file accessLimiter.
     *
     * @param clientId int
     */
    void createFileAccess(int clientId) throws InterruptedException;
}
