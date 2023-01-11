/*
 *
 */
package de.vsy.server.persistent_data.server_data;

import com.fasterxml.jackson.databind.JavaType;
import de.vsy.server.persistent_data.SynchronousFileManipulator;
import de.vsy.server.persistent_data.DataFileDescriptor;
import de.vsy.server.persistent_data.server_data.temporal.IdProviderPool;
import de.vsy.server.persistent_data.server_data.temporal.IdType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance;
import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_CLIENT_ID;

/**
 * Creates new unique client ids by reading the last unused id from a file, incrementing and
 * rewriting the incremented id. The read id is returned to the calling object.
 */
public class IdProvider extends ServerDAO {

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
        IdProviderPool idPool;
        int newId;

        if (!super.dataProvider.acquireAccess(false)) {
            LOGGER.error("No exclusive write access.");
            return STANDARD_CLIENT_ID;
        }
        idPool = readIdProvider();
        newId = idPool.getNextId(forType);
        super.dataProvider.writeData(idPool);
        super.dataProvider.releaseAccess(false);
        return newId;
    }

    public void returnId(IdType type, int idToReturn) {
        IdProviderPool idPool;

        if (!super.dataProvider.acquireAccess(false)) {
            LOGGER.error("No exclusive write access.");
        }
        idPool = readIdProvider();
        idPool.returnUnusedId(type, idToReturn);
        super.dataProvider.writeData(idPool);
        super.dataProvider.releaseAccess(false);
    }

    /**
     * Read id map.
     *
     * @return the map
     */
    private IdProviderPool readIdProvider() {
        IdProviderPool noPool = null;
        Object fromFile;
        if (!super.dataProvider.acquireAccess(false)) {
            LOGGER.error("No shared read access.");
            return null;
        }
        fromFile = super.dataProvider.readData();
        super.dataProvider.releaseAccess(false);

        if (fromFile instanceof final IdProviderPool idPool) {
            return idPool;
        }
        return null;
    }
}
