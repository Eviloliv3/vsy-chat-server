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
    public boolean registerClient(CommunicatorData clientData) {
        this.localClientDataManager.setCommunicatorData(clientData);
        return changeLocalClientState(AUTHENTICATED, true);
    }

    @Override
    public ClientState reconnectClient(CommunicatorData clientData) {
        final var currentState = persistentClientStates.getClientState(clientData.getCommunicatorId());
        final var clientState = currentState.getCurrentState();

        if (clientState != null) {
            this.localClientDataManager.setCommunicatorData(clientData);
            var dependentStateProvider = DependentClientStateProvider.getDependentStateProvider(clientState) ;
            final var dependentStates = dependentStateProvider.getDependentStatesForSubscription(true);
            dependentStates.forEach(state -> changeLocalClientState(state,true));
        }
        return clientState;
    }

    @Override
    public void deregisterClient() {
        this.changeLocalClientState(AUTHENTICATED, false);
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
        return persistentClientStates.getClientState(clientId).getCurrentState();
    }

    @Override
    public boolean changeLocalClientState(final ClientState clientState, final boolean changeTo) {
        return this.localClientStateManager.changeClientState(clientState, changeTo);
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
