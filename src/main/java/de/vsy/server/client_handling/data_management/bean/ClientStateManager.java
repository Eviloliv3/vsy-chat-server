package de.vsy.server.client_handling.data_management.bean;

import de.vsy.server.client_management.ClientState;

import java.util.*;

public class ClientStateManager implements LocalClientStateProvider {

    private final List<ClientStateListener> stateListeners;
    private final Deque<ClientState> currentState;
    private int clientStateCounter;
    private boolean stateChanged;

    public ClientStateManager() {
        this.stateListeners = new ArrayList<>(3);
        this.currentState = new ArrayDeque<>(2);
        this.clientStateCounter = 0;
        this.stateChanged = false;
    }

    /**
     * Adds the state listener.
     *
     * @param newListener the new listener
     */
    public void addStateListener(final ClientStateListener newListener) {
        this.stateListeners.add(newListener);
    }

    public Deque<ClientState> getCurrentState() {
        return this.currentState;
    }

    @Override
    public boolean checkClientState(ClientState toCheck) {
        return this.currentState.contains(toCheck);
    }

    @Override
    public boolean clientStateHasChanged() {
        boolean stateWasChanged = this.stateChanged;
        this.stateChanged = false;
        return stateWasChanged;
    }

    @Override
    public boolean clientStateHasRisen() {
        boolean stateCountHasRisen = this.clientStateCounter < this.currentState.size();
        this.clientStateCounter = this.currentState.size();
        return stateCountHasRisen;
    }

    public boolean changeClientState(final ClientState toChange, boolean toAdd) {

        if (changeCurrentState(toChange, toAdd)) {
            for (final ClientStateListener listener : stateListeners) {
                listener.evaluateNewState(toChange, toAdd);
            }
            this.stateChanged = true;
            return true;
        }
        return false;
    }

    private boolean changeCurrentState(final ClientState toChange, boolean toAdd) {
        if (toAdd) {

            if (!(this.currentState.contains(toChange))) {
                return this.currentState.add(toChange);
            } else {
                return false;
            }
        } else {
            return this.currentState.remove(toChange);
        }
    }
}
