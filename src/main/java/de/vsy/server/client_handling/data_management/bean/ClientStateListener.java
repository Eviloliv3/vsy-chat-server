package de.vsy.server.client_handling.data_management.bean;

import de.vsy.server.client_management.ClientState;

@FunctionalInterface
public interface ClientStateListener {

    /**
     * Let's listener evaluate client state changes.
     *
     * @param changedState ClientState
     * @param added        boolean
     */
    void evaluateNewState(ClientState changedState, boolean added);
}
