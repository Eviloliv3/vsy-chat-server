package de.vsy.server.client_handling.data_management.bean;

import de.vsy.server.client_management.ClientState;

public interface LocalClientStateProvider {

    /**
     * Checks whether the client is in a particular state.
     *
     * @param toCheck the ClientState to check.
     * @return true, if client is in specified ClientState, false otherwise.
     */
    boolean checkClientState(ClientState toCheck);

    /**
     * Returns client state changes once per change.
     *
     * @return true, if client state change since last check; false otherwise
     */
    boolean clientStateHasChanged();

    /**
     * Returns if new client state was added once.
     *
     * @return true, if client state change was added since last check; false otherwise
     */
    boolean clientStateHasRisen();
}
