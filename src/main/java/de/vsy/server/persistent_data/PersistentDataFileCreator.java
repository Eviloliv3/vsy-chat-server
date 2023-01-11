/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.vsy.server.persistent_data;

import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

import static java.util.Arrays.asList;

/**
 * Simple tool for creating preset file types. Assuming the passed directories have already been
 * created.
 */
public class PersistentDataFileCreator {

    /**
     * Creates the or get file.
     *
     * @param directoryNames the directory names
     * @param filename       the filename
     * @param LOGGER         the LOGGER
     * @return Array of Paths if files could be created or file paths already existed.
     * @throws IllegalStateException if IOException occurred during file creation.
     */
    public static Path[] createAndGetFilePaths(final String[] directoryNames, final String filename,
                                               final Logger LOGGER) throws IllegalStateException {
        Path[] files;
        final int fileCount = directoryNames.length;
        files = new Path[fileCount];

        for (var i = 0; i < fileCount; i++) {
            final Path currentReference = createAndGetFilePath(directoryNames[i], filename, LOGGER);

            if (currentReference == null) {
                files = null;
                break;
            } else {
                files[i] = currentReference;
            }
        }
        return files;
    }

    /**
     * Creates the or get file.
     *
     * @param directoryName  the directory name
     * @param filename       the filename
     * @param LOGGER         the LOGGER
     * @return Path if file could be created or file path already existed.
     * @throws IllegalStateException if file location could not be accessed or created.
     */
    public static Path createAndGetFilePath(final String directoryName, final String filename,
                                            final Logger LOGGER) throws IllegalStateException{
        Path fileReference = null;
        final var directory = new File(directoryName);

        try {

            if (directory.isDirectory()) {
                fileReference = Path.of(directoryName, filename);
                final var file = fileReference.toFile();

                if (!file.isFile()) {
                    var newFileIsCreated = file.createNewFile();

                    if (!newFileIsCreated) {
                        LOGGER.error("Tried to create file {}, but it already exists.",
                                fileReference);
                    }
                    return fileReference;
                } else {
                    LOGGER.trace("File already exists: {}", fileReference);
                }
            } else {
                LOGGER.error("Path is no valid directory: {}", directory);
            }
        } catch (final IOException ex) {
            throw new IllegalStateException(ex.getClass().getSimpleName() +
                    " occurred during file creation.\n" +
                    Arrays.asList(ex.getStackTrace()));
        }
        return fileReference;
    }

}
