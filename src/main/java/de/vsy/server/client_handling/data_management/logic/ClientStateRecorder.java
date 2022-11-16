package de.vsy.server.client_handling.data_management.logic;

import de.vsy.server.client_handling.data_management.bean.ClientDataManager;
import de.vsy.server.client_handling.data_management.bean.ClientStateManager;
import de.vsy.server.persistent_data.data_bean.CommunicatorData;
import de.vsy.server.persistent_data.server_data.temporal.LiveClientStateDAO;
import de.vsy.server.client_management.ClientState;
import de.vsy.server.data.socketConnection.LocalServerConnectionData;

public class ClientStateRecorder implements AuthenticationStateControl {

  private static LiveClientStateDAO persistentClientStates;
  private static LocalServerConnectionData serverNode;
  private final ClientStateManager localClientStateManager;
  private final ClientDataManager localClientDataManager;

  public ClientStateRecorder(final ClientStateManager localClientStateManager,
      final ClientDataManager localClientDataManager) {
    this.localClientStateManager = localClientStateManager;
    this.localClientDataManager = localClientDataManager;
  }

  public static void setupStaticServerDataAccess(
      final LiveClientStateDAO persistentClientStateAccess,
      final LocalServerConnectionData localServerConnectionData) {
    persistentClientStates = persistentClientStateAccess;
    serverNode = localServerConnectionData;
  }

  @Override
  public boolean loginClient(CommunicatorData clientData) {
    this.localClientDataManager.setCommunicatorData(clientData);
    return changeClientState(ClientState.AUTHENTICATED, true);
  }

  @Override
  public ClientState reconnectClient(CommunicatorData clientData) {
    final var currentState = persistentClientStates.getClientState(clientData.getCommunicatorId());
    final var clientState = currentState.getCurrentState();

    if (!(clientState.equals(ClientState.OFFLINE))) {
      this.localClientDataManager.setCommunicatorData(clientData);
      changeClientState(clientState, true);
    }
    return clientState;
  }

  @Override
  public void logoutClient() {
    final var statesToRemove = this.localClientStateManager.getCurrentState();
    for (int stateIndex = (statesToRemove.size() - 1); stateIndex >= 0; stateIndex--) {
      changeClientState(statesToRemove.get(stateIndex), false);
    }
    this.localClientDataManager.setCommunicatorData(null);
  }

  @Override
  public boolean changePendingState(boolean isPending) {
    final var clientId = this.localClientDataManager.getClientId();
    return persistentClientStates.changeClientPendingState(clientId, isPending);
  }

  @Override
  public boolean getPendingState() {
    final var clientId = this.localClientDataManager.getClientId();
    return persistentClientStates.getClientPendingState(clientId);
  }

  @Override
  public boolean changeReconnectionState(boolean newState) {
    final var clientId = this.localClientDataManager.getClientId();
    return persistentClientStates.changeReconnectionState(clientId, newState);
  }

  @Override
  public boolean getReconnectionState() {
    final var clientId = this.localClientDataManager.getClientId();
    return persistentClientStates.getClientReconnectionState(clientId);
  }

  @Override
  public ClientState getPersistentClientState() {
    final var clientId = this.localClientDataManager.getClientId();
    return persistentClientStates.getClientState(clientId).getCurrentState();
  }

  @Override
  public boolean changeClientState(final ClientState clientState, final boolean changeTo) {
    return this.localClientStateManager.changeClientState(clientState, changeTo);
  }

  @Override
  public boolean changePersistentClientState(final ClientState clientState,
      final boolean changeTo) {
    final var clientId = this.localClientDataManager.getClientId();

    if (clientState.equals(ClientState.AUTHENTICATED) && !changeTo) {
      return persistentClientStates.removeClientState(clientId);
    } else {
      return persistentClientStates.changeClientState(serverNode.getServerId(), clientId,
          clientState);
    }
  }
}
