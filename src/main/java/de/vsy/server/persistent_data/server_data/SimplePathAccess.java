package de.vsy.server.persistent_data.server_data;

import de.vsy.server.persistent_data.FileAccessRemover;

/**
 * Initiates access to persistent server data during runtime.
 */
public interface SimplePathAccess extends FileAccessRemover {

    /**
     * Creates file access using basic path creation.
     *
     * @throws IllegalStateException if any condition arises that makes the file access creation impossible.
     */
    void createFileAccess() throws IllegalStateException;
}
