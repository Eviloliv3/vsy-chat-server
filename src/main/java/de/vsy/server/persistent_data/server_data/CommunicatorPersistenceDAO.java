/*
 *
 */
package de.vsy.server.persistent_data.server_data;

import com.fasterxml.jackson.databind.JavaType;
import de.vsy.server.persistent_data.DataFileDescriptor;
import de.vsy.server.persistent_data.data_bean.CommunicatorData;

import java.util.HashSet;
import java.util.Set;

import static com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance;

public class CommunicatorPersistenceDAO extends ServerDAO {

    /**
     * Instantiates a new communicator dataManagement accessLimiter provider.
     */
    public CommunicatorPersistenceDAO() {
        super(DataFileDescriptor.COMMUNICATORS, getDataFormat());
    }

    /**
     * Returns the dataManagement format.
     *
     * @return the dataManagement format
     */
    public static JavaType getDataFormat() {
        return defaultInstance().constructCollectionType(HashSet.class, CommunicatorData.class);
    }

    /**
     * Adds the contact to list.
     *
     * @param commData the comm dataManagement
     * @return true, if successful
     */
    public boolean addCommunicator(final CommunicatorData commData) {
        Set<CommunicatorData> communicatorList;
        var communicatorAdded = false;

        if (!super.dataProvider.acquireAccess(false)) {
            LOGGER.error("No exclusive write access.");
            return false;
        }
        communicatorList = readRegisteredCommunicators();

        if (communicatorList.add(commData)) {
            communicatorAdded = super.dataProvider.writeData(communicatorList);
        } else {
            LOGGER.error(
                    "CommunicatorData was not written. Collides with existing communicator display name.");
        }
        super.dataProvider.releaseAccess(false);

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
    public Set<CommunicatorData> readRegisteredCommunicators() {
        Object fromFile;
        Set<CommunicatorData> readList = new HashSet<>();

        if (!super.dataProvider.acquireAccess(true)) {
            LOGGER.error("No shared read access.");
            return readList;
        }
        fromFile = super.dataProvider.readData();
        super.dataProvider.releaseAccess(true);

        if (fromFile instanceof HashSet) {

            try {
                readList = (Set<CommunicatorData>) fromFile;
            } catch (final ClassCastException cc) {
                LOGGER.info(
                        "{} while reading the registered client map. Empty map will be returned.",
                        cc.getClass().getSimpleName());
            }
        }
        return readList;
    }

    /**
     * Removes the communicator.
     *
     * @param communicatorId the communicator id
     * @return true, if successful
     */
    public boolean removeCommunicator(final int communicatorId) {
        var communicatorRemoved = false;
        CommunicatorData communicatorToRemove;

        if (!super.dataProvider.acquireAccess(false)) {
            LOGGER.error("No exclusive write access.");
            return false;
        }
        communicatorToRemove = getCommunicatorData(communicatorId);
        communicatorRemoved = removeCommunicator(communicatorToRemove);

        super.dataProvider.releaseAccess(false);

        return communicatorRemoved;
    }

    /**
     * Returns the communicator dataManagement.
     *
     * @param communicatorId the communicator id
     * @return the communicator dataManagement
     */
    public CommunicatorData getCommunicatorData(final int communicatorId) {
        CommunicatorData foundCommunicator = null;
        Set<CommunicatorData> communicatorList;

        if (!super.dataProvider.acquireAccess(true)) {
            LOGGER.error("No shared read access.");
            return null;
        }
        communicatorList = readRegisteredCommunicators();
        super.dataProvider.releaseAccess(true);

        for (final CommunicatorData CommunicatorData : communicatorList) {

            if (CommunicatorData.getCommunicatorId() == communicatorId) {
                foundCommunicator = CommunicatorData;
                break;
            }
        }
        return foundCommunicator;
    }

    /**
     * Removes the contact from list.
     *
     * @param clientAuthData the to delete
     * @return true, if successful
     */
    public boolean removeCommunicator(final CommunicatorData clientAuthData) {
        var communicatorRemoved = false;
        Set<CommunicatorData> communicatorList;

        if (!super.dataProvider.acquireAccess(false)) {
            LOGGER.error("No exclusive write access.");
            return false;
        }
        communicatorList = readRegisteredCommunicators();
        communicatorRemoved = (communicatorList.remove(clientAuthData)
                && super.dataProvider.writeData(communicatorList));

        super.dataProvider.releaseAccess(false);

        if (communicatorRemoved) {
            LOGGER.info("Communicator removed.");
        }

        return communicatorRemoved;
    }
}
