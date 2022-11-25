/*
 *
 */
package de.vsy.server.data.access;

import de.vsy.server.data.ServerPersistentDataManager;
import de.vsy.server.persistent_data.data_bean.AuthenticationData;
import de.vsy.server.persistent_data.data_bean.CommunicatorData;
import de.vsy.server.persistent_data.server_data.ClientAuthPersistenceDAO;
import de.vsy.server.persistent_data.server_data.CommunicatorPersistenceDAO;
import de.vsy.server.persistent_data.server_data.IdProvider;
import de.vsy.server.persistent_data.server_data.temporal.IdType;
import de.vsy.shared_transmission.dto.CommunicatorDTO;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Provides connected client thread's accessLimiter to other clients dataManagement.
 */
public class CommunicatorDataManipulator {

  private static final Logger LOGGER = LogManager.getLogger();
  private final ReadWriteLock lock;
  private final ClientAuthPersistenceDAO clientAuthPersist;
  private final CommunicatorPersistenceDAO communicatorDataPersist;
  private final IdProvider idPersist;

  /**
   * Instantiates a new client dataManagement provider.
   *
   * @param persistenceDAO the persistenceDAO accessLimiter
   */
  public CommunicatorDataManipulator(final ServerPersistentDataManager persistenceDAO) {
    this.lock = new ReentrantReadWriteLock();
    this.clientAuthPersist = persistenceDAO.getClientAuthenticationAccessManager();
    this.communicatorDataPersist = persistenceDAO.getCommunicationEntityAccessManager();
    this.idPersist = persistenceDAO.getIdProvider();
  }

  /**
   * Creates the new account.
   *
   * @param loginName   the login name
   * @param password    the password
   * @param displayName the display name
   * @return the communicator dataManagement
   */
  public CommunicatorData createNewAccount(final String loginName, final String password,
      final String displayName) {
    AuthenticationData authData;
    CommunicatorData communicatorData = null;
    final var clientId = this.idPersist.getNewId(IdType.CLIENT);

    authData = AuthenticationData.valueOf(loginName, password, clientId);

    if (this.clientAuthPersist.saveAccountData(authData)) {
      communicatorData = CommunicatorData.valueOf(clientId, clientId, displayName);

      if (!this.communicatorDataPersist.addCommunicator(communicatorData)) {
        this.clientAuthPersist.removeAccountData(authData.getClientId());
        LOGGER.error(
            "Communicator data could not be saved. Authentication data has been removed");
        communicatorData = null;
      } else {
        LOGGER.info("Account creation successful:\n{}\n{}", authData, communicatorData);
      }
    } else {
      LOGGER.error("Authentication data could not be saved.");
    }
    return communicatorData;
  }

  /**
   * Creates a new communicator, if valid owner/client id was provided.
   *
   * @param ownerId   the owner id
   * @param groupName the group name
   * @return the communicator dataManagement
   */
  public CommunicatorData createNewGroup(final int ownerId, final String groupName) {
    CommunicatorData communicatorData = null;

    if (this.clientAuthPersist.checkClientId(ownerId)) {
      final var groupId = this.idPersist.getNewId(IdType.GROUP);
      communicatorData = CommunicatorData.valueOf(groupId, ownerId, groupName);

      if (!this.communicatorDataPersist.addCommunicator(communicatorData)) {
        communicatorData = null;
      } else {
        LOGGER.error(
            "Group \"{}:{}\" creation failed. Group with same name already exists.",
            ownerId,
            groupName);
      }
    } else {
      LOGGER.error("Group \"{}:{}\" creation failed. Provided owner id is invalid.",
          ownerId,
          groupName);
    }
    return communicatorData;
  }

  /**
   * Delete account.
   *
   * @param clientId the client id
   * @return true, if successful
   */
  public boolean deleteAccount(final int clientId) {
    this.communicatorDataPersist.removeCommunicator(clientId);
    return this.clientAuthPersist.removeAccountData(clientId);
  }

  public CommunicatorData getCommunicatorData(final int communicatorId) {
    CommunicatorData foundCommunicatorData;

    try {
      this.lock.readLock().lock();
      foundCommunicatorData = this.communicatorDataPersist.getCommunicatorData(communicatorId);
    } finally {
      this.lock.readLock().unlock();
    }
    return foundCommunicatorData;
  }

  /**
   * Gets the client dataManagement.
   *
   * @param communicatorData the communicator dataManagement
   * @return the client communicator dataManagement
   */
  public CommunicatorData getCommunicatorData(final CommunicatorDTO communicatorData) {
    CommunicatorData foundCommunicatorData = null;

    if (communicatorData != null) {

      try {
        this.lock.readLock().lock();
        foundCommunicatorData = this.communicatorDataPersist
            .getCommunicatorData(communicatorData.getCommunicatorId());

        if (foundCommunicatorData != null
            && !foundCommunicatorData.getDisplayName().equals(communicatorData.getDisplayLabel())) {
          foundCommunicatorData = null;
        }
      } finally {
        this.lock.readLock().unlock();
      }
    }
    return foundCommunicatorData;
  }

  /**
   * Gets the client communicator dataManagement for credentials.
   *
   * @param loginName the login name
   * @param password  the password
   * @return the client communicator dataManagement
   */
  public CommunicatorData getCommunicatorData(final String loginName, final String password) {
    CommunicatorData communicatorData = null;

    try {
      this.lock.readLock().lock();

      if (loginName != null && password != null) {
        final var clientId = this.clientAuthPersist.getClientId(loginName, password);
        communicatorData = this.communicatorDataPersist.getCommunicatorData(clientId);
      }
    } finally {
      this.lock.readLock().unlock();
    }

    return communicatorData;
  }
}
