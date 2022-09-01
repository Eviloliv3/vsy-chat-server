/*
 *
 */
package de.vsy.server.persistent_data.server_data;

import com.fasterxml.jackson.databind.JavaType;
import de.vsy.server.persistent_data.PersistenceDAO;
import de.vsy.server.persistent_data.PersistentDataFileCreator.DataFileDescriptor;
import de.vsy.shared_module.shared_module.data_element_validation.IdCheck;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

import static com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance;

/**
 * Creates new uniqü client ids by reading the last unused id from a file,
 * incrementing and rewriting the incremented id. The read id is returned to the
 * calling object.
 */
public
class IdProvider implements ServerDataAccess {

    private static final Logger LOGGER = LogManager.getLogger();
    private final PersistenceDAO dataProvider;

    public
    IdProvider () {

        this.dataProvider = new PersistenceDAO(DataFileDescriptor.ID_MAP,
                                               getDataFormat());
    }

    /**
     * Gets the dataManagement format.
     *
     * @return the dataManagement format
     */
    public static
    JavaType getDataFormat () {
        final var factory = defaultInstance();
        return factory.constructMapType(HashMap.class, String.class, Integer.class);
    }

    @Override
    public
    void createFileAccess ()
    throws InterruptedException {
        this.dataProvider.createFileReferences();
    }

    /**
     * Gets the new communicator id.
     *
     * @return the new communicator id
     */
    public
    int getNewId () {
        Map<String, Integer> idMap;
        int newId;

        final var lockAlreadyAcquired = this.dataProvider.checkForActiveLock();

        if (!lockAlreadyAcquired) {
            this.dataProvider.acquireAccess(true);
        }
        idMap = readIdMap();
        newId = idMap.get("client");

        if (IdCheck.checkData(newId).isPresent()) {
            newId = 15000;
            LOGGER.warn("IDs zurückgesetzt.");
        }
        idMap.put("client", newId + 1);
        this.dataProvider.writeData(idMap);

        if (!lockAlreadyAcquired) {
            this.dataProvider.releaseAccess();
        }

        return newId;
    }

    /**
     * Read id map.
     *
     * @return the map
     */
    @SuppressWarnings("unchecked")
    Map<String, Integer> readIdMap () {
        Object fromFile;
        var readMap = new HashMap<String, Integer>();
        final var lockAlreadyAcquired = this.dataProvider.checkForActiveLock();

        if (!lockAlreadyAcquired) {
            this.dataProvider.acquireAccess(false);
        }
        fromFile = this.dataProvider.readData();

        if (!lockAlreadyAcquired) {
            this.dataProvider.releaseAccess();
        }

        if (fromFile instanceof HashMap) {

            try {
                readMap = (HashMap<String, Integer>) fromFile;
            } catch (final ClassCastException cc) {
                LOGGER.info(
                        "ClassCastException beim Lesen der Id-Map. Die Map wird leer ausgegeben.");
            }
        }
        return readMap;
    }

    @Override
    public
    void removeFileAccess () {
        this.dataProvider.removeFileReferences();
    }
}