package de.vsy.server.persistent_data.client_data;

import de.vsy.server.persistent_data.FileAccessRemover;

/**
 *
 */
public interface ExtendedPathAccess extends FileAccessRemover {

    /**
     * Creates file access using specified path extension.
     *
     * @param pathExtension the path extension
     * @throws IllegalArgumentException if the specified argument is not valid for file/directory creation.
     * @throws IllegalStateException    if any condition arises that makes the file access creation impossible.
     */
    void createAccess(String pathExtension) throws IllegalArgumentException, IllegalStateException;
}
