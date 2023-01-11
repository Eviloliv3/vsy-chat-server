package de.vsy.server.persistent_data.client_data;

import de.vsy.server.persistent_data.PersistentDataAccess;

/**
 * Provides means for client data access setup during runtime.
 */
public interface ClientDataAccess extends PersistentDataAccess {

    /**
     * Creates file access using specified argument. Data is then accessible.
     *
     * @param clientId the client's id
     * @throws IllegalArgumentException if the specified argument is not valid for file/directory creation.
     * @throws IllegalStateException    if any condition arises that makes the file access creation impossible.
     */
    void createFileAccess(int clientId) throws IllegalArgumentException, IllegalStateException;
}
