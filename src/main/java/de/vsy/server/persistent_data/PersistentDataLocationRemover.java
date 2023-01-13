package de.vsy.server.persistent_data;

import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PersistentDataLocationRemover {
    private PersistentDataLocationRemover() {
    }

    public static void deleteDirectories(final DataPathType pathType, final String pathExtension, Logger logger) {
        final var directories = PersistentDataLocationCreator.createDirectoryPaths(pathType, pathExtension);

        if (directories != null) {
            final var directoryCount = directories.length;
            Path[] directoryPaths = new Path[directoryCount];

            for (int directoryIndex = 0; directoryIndex < directoryCount; directoryIndex++) {
                directoryPaths[directoryIndex] = Path.of(directories[directoryIndex]);
            }
            deleteDirectories(directoryPaths, logger);
        } else {
            logger.error("No eligible directory paths could be created.");
        }
    }

    public static void deleteDirectories(Path[] filePaths, Logger logger) {
        for (final var path : filePaths) {
            final var file = path.toFile();

            try {
                if (file.isDirectory())
                    deleteDirectoryContent(file);
                file.delete();
                Files.deleteIfExists(path);
            } catch (SecurityException se) {
                logger.warn("Deletion not allowed for file/directory: {}", path);
            } catch (IOException e) {
                logger.warn("Deletion failed for: {}", path);
            }
        }
    }

    private static void deleteDirectoryContent(File directory) throws SecurityException {
        File[] files = directory.listFiles();

        if (files == null) {
            return;
        }

        for (final var file : files) {
            if (file.isDirectory())
                deleteDirectoryContent(file);
            file.delete();
        }
    }
}
