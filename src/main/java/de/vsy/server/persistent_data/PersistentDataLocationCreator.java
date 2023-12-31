/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.vsy.server.persistent_data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.EnumMap;

import static de.vsy.server.persistent_data.PersistentDataLocationCreator.DataPathDescriptor.*;
import static java.io.File.separator;
import static java.lang.System.getProperty;

/**
 * Simple tool for creating preset directories.
 */
public class PersistentDataLocationCreator {

    private static final Logger LOGGER;
    private static final String BASE_LOCATION;
    private static final EnumMap<DataPathDescriptor, String> DATA_LOCATION_PREFIXES;

    static {
        LOGGER = LogManager.getLogger();
        BASE_LOCATION =
                getProperty("user.home") + separator + "Software Development" + separator + "Programming"
                        + separator + "VSY_ChatServer" + separator + "VSY_Chat_Server_Daten" + separator;
        DATA_LOCATION_PREFIXES = new EnumMap<>(DataPathDescriptor.class);
        DATA_LOCATION_PREFIXES.put(BACKUP, "backup");
        DATA_LOCATION_PREFIXES.put(CLIENT_PATH, "clientData");
        DATA_LOCATION_PREFIXES.put(SERVER_PATH, "data");
        LOGGER.info("Working directory: {}", BASE_LOCATION);
    }

    /**
     * Creates the client dataManagement paths.
     *
     * @param pathExtension the path extension
     * @return the string[]
     * @throws IllegalStateException if attempts at opening or creating directories
     *                               cause SecurityException
     */
    public static String[] createDirectoryPaths(DataPathType pathType,
                                                final String pathExtension)
            throws IllegalStateException {
        String[] clientDataPaths = new String[2];
        clientDataPaths[0] = createStandardDirectoryPath(pathType, pathExtension);
        clientDataPaths[1] = createBackUpDirectoryPath(pathType, pathExtension);

        if (createDirectoryPaths(clientDataPaths)) {
            return clientDataPaths;
        } else {
            return null;
        }
    }

    private static String createStandardDirectoryPath(DataPathType pathType,
                                                      final String pathExtension) {
        final var directory = createBasePath(pathType, pathExtension, false);
        return finalizeDirectoryPath(directory);
    }

    private static String createBackUpDirectoryPath(DataPathType pathType,
                                                    final String pathExtension) {
        final var backUpDirectoryPath = createBasePath(pathType, pathExtension, true);
        return finalizeDirectoryPath(backUpDirectoryPath);
    }

    /**
     * Creates the directory paths.
     *
     * @param dataPaths the dataManagement paths
     * @return true, if successful
     */
    private static boolean createDirectoryPaths(final String[] dataPaths)
            throws IllegalStateException {
        boolean directoriesCreated = false;

        for (var i = (dataPaths.length - 1); i >= 0; i--) {
            directoriesCreated |= createDirectoryPath(dataPaths[i]);
        }
        return directoriesCreated;
    }

    private static StringBuilder createBasePath(final DataPathType pathType,
                                                final String pathExtension,
                                                final boolean isBackUpPath) {
        StringBuilder directory = new StringBuilder();

        if (isBackUpPath) {
            directory.append(DATA_LOCATION_PREFIXES.get(BACKUP)).append(separator);
        }

        if (pathType.equals(DataPathType.SIMPLE)) {
            directory.append(DATA_LOCATION_PREFIXES.get(SERVER_PATH));
        } else {
            directory.append(DATA_LOCATION_PREFIXES.get(CLIENT_PATH));
        }

        if (pathExtension != null && !pathExtension.isEmpty()) {
            directory.append(separator).append(pathExtension);
        }
        return directory;
    }

    private static String finalizeDirectoryPath(StringBuilder pathname) {
        return BASE_LOCATION + pathname.toString();
    }

    private static boolean createDirectoryPath(final String dataPath) throws IllegalStateException {
        boolean directoryCreated = false;
        final var directoryPath = new File(dataPath);
        try {

            if (!directoryPath.isDirectory()) {

                if (!directoryPath.mkdirs()) {
                    LOGGER.error("Directory was not created: {}", directoryPath);
                } else {
                    directoryCreated = true;
                }
            } else {
                LOGGER.trace("Directory already exists: {}", directoryPath);
                directoryCreated = true;
            }
        } catch (final SecurityException se) {
            final var errorMessage =
                    "Directory creation illegal: " + directoryPath + "\n"
                            + se.getMessage();
            throw new IllegalStateException(errorMessage);
        }
        return directoryCreated;
    }

    public static String createDirectoryPath(DataPathType pathType,
                                             final String pathExtension) {
        String standardDataPath = createStandardDirectoryPath(pathType, pathExtension);
        createDirectoryPath(standardDataPath);
        return standardDataPath;
    }

    /**
     * The Enum DataPathDescriptor.
     */
    public enum DataPathDescriptor {
        BACKUP, CLIENT_PATH, SERVER_PATH
    }
}
