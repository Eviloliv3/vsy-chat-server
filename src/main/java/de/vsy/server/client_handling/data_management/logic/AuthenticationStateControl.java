package de.vsy.server.client_handling.data_management.logic;

import de.vsy.server.client_management.ClientState;
import de.vsy.server.persistent_data.data_bean.CommunicatorData;

public interface AuthenticationStateControl extends ClientStateControl {

    /**
     * Firstly adds the authenticated client's communicator data to local state cache. Then adds
     * AUTHENTICATED state. This order should be followed, because other objects may observe state
     * changes and subsequently use the communicator data.
     *
     * @param clientData CommunicatorData
     */
    boolean registerClient(CommunicatorData clientData);

    /**
     * Gets persistent ClientState for CommunicatorData and reinstates
     * ClientConnectionHandler to that state, if it is not OFFLINE.
     *
     * @param clientData the client data to check
     * @return the persistent ClientState
     */
    ClientState reconnectClient(CommunicatorData clientData);

    /**
     * Resets ClientConnectionHandler to pre-AUTHENTICATED state.
     */
    void deregisterClient();

    /**
     * Changes the local client's global pending state.
     *
     * @param isPending boolean
     * @return true if global pending state could be change; false otherwise
     */
    boolean changePersistentPendingState(boolean isPending);

    /**
     * Returns the client's global pending state.
     *
     * @return boolean
     */
    boolean getPersistentPendingState();

    /**
     * Tries to change the local client's global reconnection state.
     *
     * @param newState boolean
     * @return boolean
     */
    boolean changePersistentReconnectionState(boolean newState);

    /**
     * Returns the local client's global reconnection state.
     *
     * @return boolean
     */
    boolean getPersistentReconnectionState();
}
