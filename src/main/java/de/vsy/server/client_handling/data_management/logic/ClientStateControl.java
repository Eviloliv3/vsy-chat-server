package de.vsy.server.client_handling.data_management.logic;

import de.vsy.server.client_management.ClientState;

import java.util.List;
import java.util.Stack;

public interface ClientStateControl {

    ClientState getPersistentClientState();

    Stack<ClientState> getLocalClientState();

    boolean changeClientState(final ClientState clientState, final boolean changeTo);

    boolean changePersistentClientState(ClientState clientState, final boolean changeTo);

    /**
     * Creates status message to distribute client's new state to whom it may concern.
     *
     * @param newState the ClientState to change
     * @param changeTo the flag indicating client's change to or from the specified
     *                 ClientState.
     */
    void appendStateSynchronizationPacket(ClientState newState, boolean changeTo);

    void appendSynchronizationRemovalPacketPerState();
}
