package de.vsy.server.client_handling.data_management.bean;

import de.vsy.server.server.client_management.ClientState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public
class ClientStateManager implements LocalClientStateProvider {

    private final Set<ClientStateListener> stateListeners;
    private final List<ClientState> currentState;
    private int previousStateCount;
    private boolean stateChanged;

    public
    ClientStateManager () {
        this.stateListeners = new HashSet<>();
        this.currentState = new ArrayList<>(ClientState.values().length);
        this.previousStateCount = 0;
        this.stateChanged = false;
    }

    /**
     * Adds the state listener.
     *
     * @param newListener the new listener
     */
    public
    void addStateListener (final ClientStateListener newListener) {
        this.stateListeners.add(newListener);
    }

    public
    List<ClientState> getCurrentState () {
        return this.currentState;
    }

    @Override
    public
    boolean checkClientState (ClientState toCheck) {
        return this.currentState.contains(toCheck);
    }

    @Override
    public
    boolean clientStateHasChanged () {
        boolean stateWasChanged = this.stateChanged;
        this.stateChanged = false;
        return stateWasChanged;
    }

    public
    boolean changeClientState (final ClientState toChange, boolean toAdd) {
        this.previousStateCount = this.currentState.size();

        if (changeCurrentState(toChange, toAdd)) {
            for (final ClientStateListener listener : stateListeners) {
                listener.evaluateNewState(toChange, toAdd);
            }
            this.stateChanged = true;
            return true;
        }
        return false;
    }

    private
    boolean changeCurrentState (final ClientState toChange, boolean toAdd) {
        if (toAdd) {
            if (!currentState.contains(toChange)) {
                return currentState.add(toChange);
            } else {
                return false;
            }
        } else {
            return this.currentState.remove(toChange);
        }
    }
}
