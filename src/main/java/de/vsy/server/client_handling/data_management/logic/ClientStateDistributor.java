package de.vsy.server.client_handling.data_management.logic;

import de.vsy.server.client_handling.data_management.bean.ClientDataManager;
import de.vsy.server.client_handling.data_management.bean.ClientStateManager;
import de.vsy.server.client_management.ClientState;
import de.vsy.server.client_management.DependentClientStateProvider;
import de.vsy.server.data.socketConnection.LocalServerConnectionData;
import de.vsy.server.persistent_data.data_bean.CommunicatorData;
import de.vsy.server.persistent_data.server_data.temporal.LiveClientStateDAO;

import static de.vsy.server.client_management.ClientState.AUTHENTICATED;

public class ClientStateDistributor implements AuthenticationStateControl {

    private static LiveClientStateDAO persistentClientStates;
    private static LocalServerConnectionData serverNode;
    private final ClientStateManager localClientStateManager;
    private final ClientDataManager localClientDataManager;
    private final ClientStatePublisher statePublisher;

    public ClientStateDistributor(final ClientStateManager localClientStateManager,
                                  final ClientDataManager localClientDataManager,
                                  final ClientStatePublisher statePublisher) {
        this.localClientStateManager = localClientStateManager;
        this.localClientDataManager = localClientDataManager;
        this.statePublisher = statePublisher;
    }

    public static void setupStaticServerDataAccess(
            final LiveClientStateDAO persistentClientStateAccess,
            final LocalServerConnectionData localServerConnectionData) {
        persistentClientStates = persistentClientStateAccess;
        serverNode = localServerConnectionData;
    }

    @Override
    public void registerClient(CommunicatorData clientData) {
        this.localClientDataManager.setCommunicatorData(clientData);
        changeLocalClientState(AUTHENTICATED, true);
    }

    @Override
    public ClientState reconnectClient(CommunicatorData clientData) {
        final var currentState = persistentClientStates.getClientState(clientData.getCommunicatorId());

        if (currentState != null) {
            final var clientState = currentState.getCurrentState();
            this.localClientDataManager.setCommunicatorData(clientData);
            changeLocalClientState(clientState, true);
            return clientState;
        }
        return null;
    }

    @Override
    public void deregisterClient() {
        changeLocalClientState(AUTHENTICATED, false);
        this.localClientDataManager.setCommunicatorData(null);
    }

    @Override
    public boolean changePersistentPendingState(boolean isPending) {
        final var clientId = this.localClientDataManager.getClientId();
        return persistentClientStates.changeClientPendingState(clientId, isPending);
    }

    @Override
    public boolean getPersistentPendingState() {
        final var clientId = this.localClientDataManager.getClientId();
        return persistentClientStates.getClientPendingState(clientId);
    }

    @Override
    public boolean changePersistentReconnectionState(boolean newState) {
        final var clientId = this.localClientDataManager.getClientId();
        return persistentClientStates.changeReconnectionState(clientId, newState);
    }

    @Override
    public boolean getPersistentReconnectionState() {
        final var clientId = this.localClientDataManager.getClientId();
        return persistentClientStates.getClientReconnectionState(clientId);
    }

    @Override
    public ClientState getGlobalClientState() {
        final var clientId = this.localClientDataManager.getClientId();
        final var currentState = persistentClientStates.getClientState(clientId);

        if (currentState != null) {
            return currentState.getCurrentState();
        }
        return null;
    }

    @Override
    public void changeLocalClientState(final ClientState clientState, final boolean changeTo) {
        var dependentStateProvider = DependentClientStateProvider.getDependentStateProvider(clientState);
        final var dependentStates = dependentStateProvider.getDependentStatesForSubscription(changeTo);
        dependentStates.forEach(state -> this.localClientStateManager.changeClientState(state, changeTo));
    }

    @Override
    public boolean changePersistentClientState(final ClientState clientState,
                                               final boolean changeTo) {
        final var clientId = this.localClientDataManager.getClientId();

        if (clientState.equals(AUTHENTICATED) && !changeTo) {
            return persistentClientStates.removeClientState(clientId);
        } else {
            return persistentClientStates.changeClientState(serverNode.getServerId(), clientId,
                    clientState);
        }
    }

    @Override
    public void appendStateSynchronizationPacket(ClientState newState, boolean changeTo) {
        this.statePublisher.publishStateChange(newState, changeTo);
    }

    @Override
    public void appendSynchronizationRemovalPacketPerState() {
        final var currentStates = this.localClientStateManager.getCurrentState();
        final var descendingStates = currentStates.descendingIterator();

        while (descendingStates.hasNext()) {
            appendStateSynchronizationPacket(descendingStates.next(), false);
        }
    }
}
