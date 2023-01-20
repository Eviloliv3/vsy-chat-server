package de.vsy.server.client_handling.data_management.logic;

import de.vsy.server.client_management.ClientState;

public interface ClientStateControl {

    /**
     * Returns the persistent ClientState for the currently authenticated client.
     *
     * @return the ClientState
     */
    ClientState getGlobalClientState();

    /**
     * Changes the local ClientState and every client connection related component,
     * which changes its behaviour due to the state change.
     *
     * @param clientState the ClientState to change
     * @param changeTo    flag indicating, if ClientState is new or if it should be removed.
     */
    void changeLocalClientState(final ClientState clientState, final boolean changeTo);

    /**
     * Changes the persistent/global ClientState for the currently connected client.
     *
     * @param clientState the ClientState to change
     * @param changeTo    flag indicating, if ClientState is new or if it should be removed.
     * @return true, if ClientState was successfully set, false otherwise.
     */
    boolean changePersistentClientState(ClientState clientState, final boolean changeTo);

    /**
     * Creates status message to distribute client's new state to server and contacts
     * if necessary.
     *
     * @param newState the ClientState to change
     * @param changeTo the flag indicating client's change to or from the specified
     *                 ClientState.
     */
    void appendStateSynchronizationPacket(ClientState newState, boolean changeTo);

    /**
     * Creates status messages for all states that are removed, distributing the
     * client's ClientState change to server and contacts if necessary.
     */
    void appendSynchronizationRemovalPacketPerState();
}
