/*
 *
 */
package de.vsy.server.persistent_data.server_data;

import com.fasterxml.jackson.databind.JavaType;
import de.vsy.server.persistent_data.DataFileDescriptor;
import de.vsy.server.persistent_data.DataPathType;
import de.vsy.server.persistent_data.PersistentDataLocationCreator;
import de.vsy.server.persistent_data.server_data.temporal.IdProviderPool;
import de.vsy.server.persistent_data.server_data.temporal.IdType;
import de.vsy.shared_module.data_element_validation.IdCheck;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.stream.IntStream;

import static com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance;
import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_CLIENT_ID;

/**
 * Creates new unique client ids by reading the last unused id from a file, incrementing and
 * rewriting the incremented id. The read id is returned to the calling object.
 */
public class IdProvider extends ServerDAO {

    private static final int MIN_EXCLUSIVE_CLIENT_ID = 15000;

    public IdProvider() {
        super(DataFileDescriptor.ID_MAP, getDataFormat());
    }

    /**
     * Returns the dataManagement format.
     *
     * @return the dataManagement format
     */
    public static JavaType getDataFormat() {
        final var factory = defaultInstance();
        return factory.constructType(IdProviderPool.class);
    }

    /**
     * Returns the new communicator id.
     *
     * @return the new communicator id
     */
    public int getNewId(IdType forType) {
        if (!super.dataProvider.acquireAccess(false)) {
            LOGGER.error("No exclusive write access.");
            return STANDARD_CLIENT_ID;
        }

        final var idProvider = readIdProvider();
        final var newId = idProvider.getNextId(forType);
        super.dataProvider.writeData(idProvider);
        super.dataProvider.releaseAccess(false);
        return newId;
    }

    public void returnId(IdType type, int idToReturn) {
        if (!super.dataProvider.acquireAccess(false)) {
            LOGGER.error("No exclusive write access.");
        }
        final var idProvider = readIdProvider();
        idProvider.returnUnusedId(type, idToReturn);
        super.dataProvider.writeData(idProvider);
        super.dataProvider.releaseAccess(false);
    }

    /**
     * Read id map.
     *
     * @return the map
     */
    private IdProviderPool readIdProvider() {
        if (!super.dataProvider.acquireAccess(false)) {
            LOGGER.error("No shared read access.");
            return null;
        }
        final var fromFile = super.dataProvider.readData();
        super.dataProvider.releaseAccess(false);

        if (fromFile instanceof final IdProviderPool idPool) {
            return idPool;
        }
        return createNewIdProviderPool();
    }

    private IdProviderPool createNewIdProviderPool() {
        final LinkedList<Integer> clientPool;
        final int newIdCounter;
        final var idCounters = new EnumMap<IdType, Integer>(IdType.class);
        final var idPools = new EnumMap<IdType, Queue<Integer>>(IdType.class);
        var lastUsedClientId = getMaxClientId();
        final var idErrorMessage = IdCheck.checkData(lastUsedClientId);

        if (idErrorMessage.isPresent()) {
            lastUsedClientId = MIN_EXCLUSIVE_CLIENT_ID;
        }
        newIdCounter = lastUsedClientId + 11;
        clientPool = new LinkedList<>(IntStream.range(lastUsedClientId + 1, newIdCounter).boxed().toList());
        idCounters.put(IdType.CLIENT, newIdCounter);
        idPools.put(IdType.CLIENT, clientPool);
        return IdProviderPool.instantiate(idCounters, idPools);
    }

    private int getMaxClientId() {
        int maxClientId = -1;
        var clientDataPath = PersistentDataLocationCreator.createDirectoryPath(DataPathType.EXTENDED, String.valueOf(0));
        final var lastBackSlashIndex = clientDataPath.lastIndexOf("/");
        clientDataPath = clientDataPath.substring(0, lastBackSlashIndex);
        final var clientDataFile = Path.of(clientDataPath).toFile();

        if (clientDataFile.isDirectory()) {
            final var subdirectories = clientDataFile.listFiles();
            for (final var directory : subdirectories) {
                if (directory.isDirectory()) {
                    try {
                        int currentClientId = Integer.parseInt(directory.getName());

                        if (maxClientId < currentClientId) {
                            maxClientId = currentClientId;
                        }
                    } catch (NumberFormatException nfe) {
                        LOGGER.error("Not a client directory: {}", directory.getAbsolutePath());
                    }
                }
            }
        }
        return maxClientId;
    }
}
