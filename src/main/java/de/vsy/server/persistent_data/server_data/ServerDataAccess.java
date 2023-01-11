package de.vsy.server.persistent_data.server_data;

import de.vsy.server.persistent_data.PersistentDataAccess;

/**
 * Initiates access to persistent server data during runtime.
 */
public interface ServerDataAccess extends PersistentDataAccess {

    /**
     * Creates file access. Data is then accessible.
     *
     * @throws IllegalStateException if any condition arises that makes the file access creation impossible.
     */
    void createFileAccess() throws IllegalStateException;
}
