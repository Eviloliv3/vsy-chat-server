/*
 *
 */
package de.vsy.server.persistent_data.server_data;

import static com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance;

import com.fasterxml.jackson.databind.JavaType;
import de.vsy.server.persistent_data.PersistenceDAO;
import de.vsy.server.persistent_data.PersistentDataFileCreator.DataFileDescriptor;
import de.vsy.server.persistent_data.data_bean.CommunicatorData;
import java.util.HashSet;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CommunicatorPersistenceDAO implements ServerDataAccess {

  private static final Logger LOGGER = LogManager.getLogger();
  private final PersistenceDAO dataProvider;

  /**
   * Instantiates a new communicator dataManagement accessLimiter provider.
   */
  public CommunicatorPersistenceDAO() {

    this.dataProvider = new PersistenceDAO(DataFileDescriptor.COMMUNICATORS, getDataFormat());
  }

  /**
   * Gets the dataManagement format.
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

    if (!this.dataProvider.acquireAccess(true)) {
      LOGGER.error("No exclusive write access.");
      return false;
    }
    communicatorList = readRegisteredCommunicators();

    if (communicatorList.add(commData)) {
      communicatorAdded = this.dataProvider.writeData(communicatorList);
    } else {
      LOGGER.error(
          "CommunicatorData was not written. Collides with existing communicator display name.");
    }
    this.dataProvider.releaseAccess(true);

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

    if (!this.dataProvider.acquireAccess(false)) {
      LOGGER.error("No shared read access.");
      return readList;
    }
    fromFile = this.dataProvider.readData();
    this.dataProvider.releaseAccess(false);

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

  @Override
  public void createFileAccess()
      throws IllegalStateException, IllegalArgumentException, InterruptedException {
    this.dataProvider.createFileReferences();
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

    if (!this.dataProvider.acquireAccess(true)) {
      LOGGER.error("No exclusive write access.");
      return false;
    }
    communicatorToRemove = getCommunicatorData(communicatorId);
    communicatorRemoved = removeCommunicator(communicatorToRemove);

    this.dataProvider.releaseAccess(true);

    return communicatorRemoved;
  }

  /**
   * Gets the communicator dataManagement.
   *
   * @param communicatorId the communicator id
   * @return the communicator dataManagement
   */
  public CommunicatorData getCommunicatorData(final int communicatorId) {
    CommunicatorData foundCommunicator = null;
    Set<CommunicatorData> communicatorList;

    if (!this.dataProvider.acquireAccess(false)) {
      LOGGER.error("No shared read access.");
      return null;
    }
    communicatorList = readRegisteredCommunicators();
    this.dataProvider.releaseAccess(false);

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

    if (!this.dataProvider.acquireAccess(true)) {
      LOGGER.error("No exclusive write access.");
      return false;
    }
    communicatorList = readRegisteredCommunicators();
    communicatorRemoved = (communicatorList.remove(clientAuthData)
        && this.dataProvider.writeData(communicatorList));

    this.dataProvider.releaseAccess(true);

    if (communicatorRemoved) {
      LOGGER.info("Communicator removed.");
    }

    return communicatorRemoved;
  }

  @Override
  public void removeFileAccess() throws IllegalStateException, IllegalArgumentException {
    this.dataProvider.removeFileReferences();
  }
}
