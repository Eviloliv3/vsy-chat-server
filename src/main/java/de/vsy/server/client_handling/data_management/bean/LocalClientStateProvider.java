package de.vsy.server.client_handling.data_management.bean;

import de.vsy.server.client_management.ClientState;

public interface LocalClientStateProvider {

    boolean checkClientState(ClientState toCheck);

    /**
     * Returns client state changes once per change.
     *
     * @return true, if client state change since last check; false otherwise
     */
    boolean clientStateHasChanged();
}
