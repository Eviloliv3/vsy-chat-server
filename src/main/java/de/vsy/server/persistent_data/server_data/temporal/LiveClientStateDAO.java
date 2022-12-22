package de.vsy.server.persistent_data.server_data.temporal;

import static com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance;
import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_SERVER_ID;

import com.fasterxml.jackson.databind.JavaType;
import de.vsy.server.client_management.ClientState;
import de.vsy.server.client_management.CurrentClientState;
import de.vsy.server.data.SocketConnectionDataManager;
import de.vsy.server.persistent_data.PersistenceDAO;
import de.vsy.server.persistent_data.PersistentDataFileCreator.DataFileDescriptor;
import de.vsy.server.persistent_data.server_data.ServerDataAccess;
import de.vsy.shared_transmission.packet.property.packet_category.PacketCategory;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Allows persistent CRUD operations on client states.
 */
public class LiveClientStateDAO implements ServerDataAccess {

  private static final Logger LOGGER = LogManager.getLogger();
  private final SocketConnectionDataManager serverConnections;
  private final PersistenceDAO dataProvider;

  public LiveClientStateDAO(final SocketConnectionDataManager serverConnections) {
    this.serverConnections = serverConnections;
    this.dataProvider = new PersistenceDAO(DataFileDescriptor.ACTIVE_CLIENTS, getDataFormat());
  }

  /**
   * Returns the dataManagement format.
   *
   * @return the java type
   */
  public static JavaType getDataFormat() {
    return defaultInstance().constructMapType(HashMap.class, Integer.class,
        CurrentClientState.class);
  }

  public Map<PacketCategory, Set<Integer>> getAllExtraSubscriptions(final int clientId) {
    final Map<PacketCategory, Set<Integer>> extraSubscriptions = new EnumMap<>(
        PacketCategory.class);
    final CurrentClientState currentState;

    if (!this.dataProvider.acquireAccess(false)) {
      LOGGER.error("No shared read access.");
      return extraSubscriptions;
    }
    currentState = getClientState(clientId);
    this.dataProvider.releaseAccess(false);

    if (currentState != null) {
      extraSubscriptions.putAll(currentState.getExtraSubscriptions());
    }
    return extraSubscriptions;
  }

  /**
   * Returns the client state.
   *
   * @param clientId the client id
   * @return the client state
   */
  public CurrentClientState getClientState(final int clientId) {
    CurrentClientState currentClientState;
    Map<Integer, CurrentClientState> clientStateMap;

    if (!this.dataProvider.acquireAccess(false)) {
      LOGGER.error("No shared read access.");
      return new CurrentClientState(STANDARD_SERVER_ID);
    }
    clientStateMap = getAllActiveClientStates();
    this.dataProvider.releaseAccess(false);
    currentClientState = clientStateMap.get(clientId);

    if (currentClientState == null) {
      currentClientState = new CurrentClientState(STANDARD_SERVER_ID);
    }
    return currentClientState;
  }

  /**
   * Returns the all active client states.
   *
   * @return Map<Integer, CurrentClientState>
   */
  @SuppressWarnings("unchecked")
  public Map<Integer, CurrentClientState> getAllActiveClientStates() {
    Map<Integer, CurrentClientState> readMap = new HashMap<>();
    Object fromFile;

    if (!this.dataProvider.acquireAccess(false)) {
      LOGGER.error("No shared read access.");
      return readMap;
    }
    fromFile = this.dataProvider.readData();
    this.dataProvider.releaseAccess(false);

    if (fromFile instanceof HashMap) {

      try {
        readMap = (Map<Integer, CurrentClientState>) fromFile;
      } catch (final ClassCastException cc) {
        LOGGER.info("{} occurred during reading the active client statuses.",
            cc.getClass().getSimpleName());
      }
    }
    return readMap;
  }

  /**
   * Change client state.
   *
   * @param serverPort the server port
   * @param clientId   the client id
   * @param newState   the new state
   * @return true, if successful
   */
  public boolean changeClientState(final int serverPort, final int clientId,
      final ClientState newState) {
    var stateChanged = false;
    CurrentClientState clientState;
    Map<Integer, CurrentClientState> clientStateMap;

    if (!this.dataProvider.acquireAccess(true)) {
      LOGGER.error("No exclusive write access.");
      return false;
    }
    clientStateMap = getAllActiveClientStates();
    clientState = clientStateMap.get(clientId);

    if (clientState == null) {
      clientState = new CurrentClientState(serverPort);
    }
    clientState.setCurrentState(newState);
    clientState.changeServerPort(serverPort);
    clientStateMap.put(clientId, clientState);
    stateChanged = this.dataProvider.writeData(clientStateMap);

    this.dataProvider.releaseAccess(true);

    if (stateChanged) {
      LOGGER.info("{}: {}", clientId, newState);
    }
    return stateChanged;
  }

  public boolean addExtraSubscription(final int clientId, PacketCategory topic,
      final int threadId) {
    CurrentClientState clientState;
    Map<Integer, CurrentClientState> clientStateMap;
    var subscriptionAdded = false;
    if (!this.dataProvider.acquireAccess(true)) {
      LOGGER.error("No exclusive write access.");
      return false;
    }
    clientStateMap = getAllActiveClientStates();
    clientState = clientStateMap.get(clientId);

    if (clientState != null) {
      clientState.updateExtraSubscription(topic, threadId, true);
      clientStateMap.put(clientId, clientState);
      subscriptionAdded = this.dataProvider.writeData(clientStateMap);
    } else {
      LOGGER.info("{} - state is not managed: extra subscription not added.",
          clientId);
    }

    this.dataProvider.releaseAccess(true);

    return subscriptionAdded;
  }

  public boolean removeExtraSubscription(final int clientId, PacketCategory topic,
      final int threadId) {
    CurrentClientState clientState;
    Map<Integer, CurrentClientState> clientStateMap;
    var subscriptionAdded = false;
    if (!this.dataProvider.acquireAccess(true)) {
      LOGGER.error("No exclusive write access.");
      return false;
    }
    clientStateMap = getAllActiveClientStates();
    clientState = clientStateMap.get(clientId);

    if (clientState != null) {
      clientState.updateExtraSubscription(topic, threadId, false);
      clientStateMap.put(clientId, clientState);
      subscriptionAdded = this.dataProvider.writeData(clientStateMap);
    } else {
      LOGGER.info("{} - state is not managed: extra subscription not removed.", clientId);
    }

    this.dataProvider.releaseAccess(true);

    return subscriptionAdded;
  }

  /**
   * Change client state.
   *
   * @param clientId     the client id
   * @param pendingState the pending state
   * @return true, if successful
   */
  public boolean changeClientPendingState(final int clientId, final boolean pendingState) {
    CurrentClientState clientState;
    Map<Integer, CurrentClientState> clientStateMap;
    var pendingStateChanged = false;
    if (!this.dataProvider.acquireAccess(true)) {
      LOGGER.error("No exclusive write access.");
      return false;
    }
    clientStateMap = getAllActiveClientStates();
    clientState = clientStateMap.get(clientId);

    if (clientState != null) {
      clientState.setPendingState(pendingState);
      clientStateMap.put(clientId, clientState);
      pendingStateChanged = this.dataProvider.writeData(clientStateMap);
    } else {
      LOGGER.info("{} - state is not managed: PendingState has not been changed.",
          clientId);
    }

    this.dataProvider.releaseAccess(true);

    return pendingStateChanged;
  }

  /**
   * Change reconnection state.
   *
   * @param clientId the client id
   * @param newState the newState
   * @return true, if successful
   */
  public boolean changeReconnectionState(final int clientId, boolean newState) {
    var reconnectStateSet = false;
    CurrentClientState clientState;
    Map<Integer, CurrentClientState> clientStateMap;

    if (!this.dataProvider.acquireAccess(true)) {
      LOGGER.error("No exclusive write access.");
      return false;
    }
    clientStateMap = getAllActiveClientStates();
    clientState = clientStateMap.get(clientId);

    if (clientState != null) {
      reconnectStateSet = clientState.setReconnectState(newState);

      if (reconnectStateSet) {
        LOGGER.trace("Reconnection state set to {}.", newState);
        clientStateMap.put(clientId, clientState);
        reconnectStateSet = this.dataProvider.writeData(clientStateMap);
      } else {
        final var clientNotReconnecting = !(clientState.getPendingState());

        if (clientNotReconnecting) {
          final var pendingStateSet = trySetRemoteClientPending(clientState);

          if (pendingStateSet) {
            LOGGER.trace("Pending state set to true for remotely connected client. Reconnection "
                + "state change will be attempted anew.");
            clientStateMap.put(clientId, clientState);
            this.dataProvider.writeData(clientStateMap);
            reconnectStateSet = changeReconnectionState(clientId, newState);
          } else {
            LOGGER.warn("Pending state could not be set, while trying to set reconnect "
                + "state to {}", newState);
          }
        } else {
          LOGGER.trace("Reconnection state not set: client already reconnecting.");
        }
      }
    } else {
      LOGGER.trace("No client state specified.");
    }
    this.dataProvider.releaseAccess(true);
    return reconnectStateSet;
  }

  private boolean trySetRemoteClientPending(CurrentClientState clientState) {

    final var clientServerId = clientState.getServerId();
    final var clientRemoteConnected =
        clientServerId != this.serverConnections.getLocalServerConnectionData().getServerId();

    if (clientRemoteConnected) {
      final var remoteServerOffline =
          this.serverConnections.getLiveServerConnection(clientServerId) == null;
      if (remoteServerOffline) {
        clientState.setPendingState(true);
        return true;
      } else {
        LOGGER.trace("Client connected remotely, but remote Server is not offline.");
      }
    } else {
      LOGGER.trace("Pending state not changed. Client not connected remotely.");
    }
    return false;
  }

  @Override
  public void createFileAccess() throws InterruptedException {
    this.dataProvider.createFileReferences();
  }

  /**
   * Returns the client pending state.
   *
   * @param clientId the client id
   * @return the client pending state
   */
  public boolean getClientPendingState(final int clientId) {
    var clientPending = false;
    CurrentClientState clientState;
    Map<Integer, CurrentClientState> clientStateMap;

    if (!this.dataProvider.acquireAccess(false)) {
      LOGGER.error("No shared read access.");
      return false;
    }
    clientStateMap = getAllActiveClientStates();
    this.dataProvider.releaseAccess(false);
    clientState = clientStateMap.get(clientId);

    if (clientState != null) {
      clientPending = clientState.getPendingState();
    }
    return clientPending;
  }

  /**
   * Returns the client pending state.
   *
   * @param clientId the client id
   * @return the client pending state
   */
  public boolean getClientReconnectionState(final int clientId) {
    var clientPending = false;
    CurrentClientState clientState;
    Map<Integer, CurrentClientState> clientStateMap;

    if (!this.dataProvider.acquireAccess(false)) {
      LOGGER.error("No shared read access.");
      return false;
    }
    clientStateMap = getAllActiveClientStates();
    this.dataProvider.releaseAccess(false);
    clientState = clientStateMap.get(clientId);

    if (clientState != null) {
      clientPending = clientState.getReconnectState();
    }
    return clientPending;
  }

  /**
   * Returns specified servers locally connected client states only.
   *
   * @param serverPort int
   * @return Map<Integer, CurrentClientState>
   */
  public Map<Integer, CurrentClientState> getClientStatesForServer(final int serverPort) {
    Map<Integer, CurrentClientState> allClientStates;
    Map<Integer, CurrentClientState> remoteClientStates = new HashMap<>();

    if (!this.dataProvider.acquireAccess(false)) {
      LOGGER.error("No shared read access.");
      return remoteClientStates;
    }
    allClientStates = getAllActiveClientStates();
    this.dataProvider.releaseAccess(false);

    for (final Map.Entry<Integer, CurrentClientState> client : allClientStates.entrySet()) {
      final var clientState = client.getValue();

      if (clientState.getServerId() == serverPort) {
        remoteClientStates.put(client.getKey(), clientState);
      }
    }
    return remoteClientStates;
  }

  /**
   * Removes the all client states.
   *
   * @return true, if successful
   */
  public boolean removeAllClientStates() {
    var clientStatesRemoved = false;
    Map<Integer, CurrentClientState> clientStateMap;

    if (!this.dataProvider.acquireAccess(true)) {
      LOGGER.error("No exclusive write access.");
      return false;
    }
    clientStateMap = new HashMap<>();
    clientStatesRemoved = this.dataProvider.writeData(clientStateMap);
    this.dataProvider.releaseAccess(true);

    if (clientStatesRemoved) {
      LOGGER.info("Client states will be removed.");
    }
    return clientStatesRemoved;
  }

  /**
   * Removes the client state.
   *
   * @param clientId the client id
   * @return true, if successful
   */
  public boolean removeClientState(final int clientId) {
    Map<Integer, CurrentClientState> clientStateMap;
    var clientStateRemoved = false;

    if (!this.dataProvider.acquireAccess(true)) {
      LOGGER.error("No exclusive write access.");
      return false;
    }
    clientStateMap = getAllActiveClientStates();
    clientStateRemoved =
        (clientStateMap.remove(clientId) != null) && this.dataProvider.writeData(clientStateMap);
    this.dataProvider.releaseAccess(true);

    if (clientStateRemoved) {
      LOGGER.info("Client removed: {} ", clientId);
    }
    return clientStateRemoved;
  }

  @Override
  public void removeFileAccess() {
    this.dataProvider.removeFileReferences();
  }
}
