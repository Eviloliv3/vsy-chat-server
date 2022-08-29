/*
 *
 */
package de.vsy.chat.server.persistent_data.server_data;

import com.fasterxml.jackson.databind.JavaType;
import de.vsy.chat.server.persistent_data.PersistenceDAO;
import de.vsy.chat.server.persistent_data.PersistentDataFileCreator.DataFileDescriptor;
import de.vsy.chat.server.persistent_data.data_bean.CommunicatorData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Set;

import static com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance;

public
class CommunicatorPersistenceDAO implements ServerDataAccess {

    private static final Logger LOGGER = LogManager.getLogger();
    private final PersistenceDAO dataProvider;

    /** Instantiates a new communicator dataManagement accessLimiter provider. */
    public
    CommunicatorPersistenceDAO () {

        this.dataProvider = new PersistenceDAO(DataFileDescriptor.COMMUNICATORS,
                                               getDataFormat());
    }

    /**
     * Gets the dataManagement format.
     *
     * @return the dataManagement format
     */
    public static
    JavaType getDataFormat () {
        return defaultInstance().constructCollectionType(HashSet.class,
                                                         CommunicatorData.class);
    }

    /**
     * Adds the contact to list.
     *
     * @param commData the comm dataManagement
     *
     * @return true, if successful
     */
    public
    boolean addCommunicator (final CommunicatorData commData) {
        Set<CommunicatorData> communicatorList;
        var communicatorAdded = false;

        final var lockAlreadyAcquired = this.dataProvider.checkForActiveLock();

        if (!lockAlreadyAcquired) {
            this.dataProvider.acquireAccess(true);
        }
        communicatorList = readRegisteredCommunicators();

        if (communicatorList.add(commData)) {
            communicatorAdded = this.dataProvider.writeData(communicatorList);
        } else {
            LOGGER.error("Kommunikatordaten wurden nicht gepeichert. Es " +
                         "existiert bereits ein Kommunikator mit demselben " +
                         "Anzeigenamen.");
        }
        if (!lockAlreadyAcquired) {
            this.dataProvider.releaseAccess();
        }

        if (communicatorAdded) {
            LOGGER.info("Communicator added.");
        }
        return communicatorAdded;
    }

    /**
     * Read registered clients.
     *
     * @return the list
     */
    @SuppressWarnings("unchecked")
    public
    Set<CommunicatorData> readRegisteredCommunicators () {
        Object fromFile;
        Set<CommunicatorData> readList = new HashSet<>();

        final var lockAlreadyAcquired = this.dataProvider.checkForActiveLock();

        if (!lockAlreadyAcquired) {
            this.dataProvider.acquireAccess(false);
        }

        fromFile = this.dataProvider.readData();

        if (!lockAlreadyAcquired) {
            this.dataProvider.releaseAccess();
        }

        if (fromFile instanceof Set) {

            try {
                readList = (Set<CommunicatorData>) fromFile;
            } catch (final ClassCastException cc) {
                LOGGER.info(
                        "ClassCastException beim Lesen der registrierten Clietns-Map. Die Map wird leer ausgegeben.");
            }
        }
        return readList;
    }

    @Override
    public
    void createFileAccess ()
    throws IllegalStateException, IllegalArgumentException, InterruptedException {
        this.dataProvider.createFileReferences();
    }

    /**
     * Removes the communicator.
     *
     * @param communicatorId the communicator id
     *
     * @return true, if successful
     */
    public
    boolean removeCommunicator (final int communicatorId) {
        var communicatorRemoved = false;
        CommunicatorData communicatorToRemove;

        final var lockAlreadyAcquired = this.dataProvider.checkForActiveLock();

        if (!lockAlreadyAcquired) {
            this.dataProvider.acquireAccess(true);
        }

        communicatorToRemove = getCommunicatorData(communicatorId);
        communicatorRemoved = removeCommunicator(communicatorToRemove);

        if (!lockAlreadyAcquired) {
            this.dataProvider.releaseAccess();
        }

        return communicatorRemoved;
    }

    /**
     * Gets the communicator dataManagement.
     *
     * @param communicatorId the communicator id
     *
     * @return the communicator dataManagement
     */
    public
    CommunicatorData getCommunicatorData (final int communicatorId) {
        CommunicatorData foundCommunicator = null;
        Set<CommunicatorData> communicatorList;

        final var lockAlreadyAcquired = this.dataProvider.checkForActiveLock();

        if (!lockAlreadyAcquired) {
            this.dataProvider.acquireAccess(false);
        }
        communicatorList = readRegisteredCommunicators();

        for (final CommunicatorData CommunicatorData : communicatorList) {

            if (CommunicatorData.getCommunicatorId() == communicatorId) {
                foundCommunicator = CommunicatorData;
                break;
            }
        }

        if (!lockAlreadyAcquired) {
            this.dataProvider.releaseAccess();
        }
        return foundCommunicator;
    }

    /**
     * Removes the contact from list.
     *
     * @param clientAuthData the to delete
     *
     * @return true, if successful
     */
    public
    boolean removeCommunicator (final CommunicatorData clientAuthData) {
        var communicatorRemoved = false;
        Set<CommunicatorData> communicatorList;

        final var lockAlreadyAcquired = this.dataProvider.checkForActiveLock();

        if (!lockAlreadyAcquired) {
            this.dataProvider.acquireAccess(true);
        }
        communicatorList = readRegisteredCommunicators();
        communicatorRemoved = (communicatorList.remove(clientAuthData) &&
                               this.dataProvider.writeData(communicatorList));

        if (!lockAlreadyAcquired) {
            this.dataProvider.releaseAccess();
        }

        if (communicatorRemoved) {
            LOGGER.info("Communicator removed.");
        }

        return communicatorRemoved;
    }

    @Override
    public
    void removeFileAccess ()
    throws IllegalStateException, IllegalArgumentException {
        this.dataProvider.removeFileReferences();
    }
}
