/*
 *
 */
package de.vsy.server.server.data.access;

import de.vsy.server.persistent_data.data_bean.AuthenticationData;
import de.vsy.server.persistent_data.data_bean.CommunicatorData;
import de.vsy.server.persistent_data.server_data.ClientAuthPersistenceDAO;
import de.vsy.server.persistent_data.server_data.CommunicatorPersistenceDAO;
import de.vsy.server.persistent_data.server_data.IdProvider;
import de.vsy.server.server.data.ServerPersistentDataManager;
import de.vsy.shared_transmission.shared_transmission.dto.CommunicatorDTO;
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
    final var clientId = this.idPersist.getNewId();

    authData = AuthenticationData.valueOf(loginName, password, clientId);

    if (this.clientAuthPersist.saveAccountData(authData)) {
      communicatorData = CommunicatorData.valueOf(clientId, clientId, displayName);

      if (!this.communicatorDataPersist.addCommunicator(communicatorData)) {
        this.clientAuthPersist.removeAccountData(authData.getClientId());
        LOGGER.error(
            "Kommunikatordaten konnten nicht " + "gespeichert werden. Authentifikationsdaten "
                + "wurden wieder entfernt");
        communicatorData = null;
      } else {
        LOGGER.info("Konto erfolgreich erstellt:\n{}\n{}", authData, communicatorData);
      }
    } else {
      LOGGER.error("Authentifizierungsdaten konnten nicht " + "gespeichert werden.");
    }
    return communicatorData;
  }

  /**
   * Erstellt einen neuen Kommunikator, sofern eine gültige Besitzer-(/Klienten-)Id angegeben
   * wurde.
   *
   * @param ownerId   the owner id
   * @param groupName the group name
   * @return the communicator dataManagement
   */
  public CommunicatorData createNewGroup(final int ownerId, final String groupName) {
    CommunicatorData communicatorData = null;

    if (this.clientAuthPersist.checkClientId(ownerId)) {
      final var groupId = this.idPersist.getNewId();
      communicatorData = CommunicatorData.valueOf(groupId, ownerId, groupName);

      if (!this.communicatorDataPersist.addCommunicator(communicatorData)) {
        communicatorData = null;
      } else {
        LOGGER.error(
            "Gruppe \"{}:{}\"wurde nicht erstellt. " + "Es gibt bereits gleichnamige Gruppe",
            ownerId,
            groupName);
      }
    } else {
      LOGGER.error("Gruppe \"{}:{}\"wurde nicht erstellt. Die " + "Klienten-Id ist ungültig.",
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
