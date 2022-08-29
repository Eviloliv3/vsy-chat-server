package de.vsy.chat.server.server.client_management;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Wird zur Synchronisation von Pendingzustand und Wiederverbindung verwendet.
 */
public
class PendingState {

    private boolean pending;
    private boolean reconnecting;

    /** Instantiates a new pending state. */
    public
    PendingState () {
        this(false, false);
    }

    /**
     * Instantiates a new pending state.
     *
     * @param pending the pending state
     * @param reconnecting the reconnecting
     */
    @JsonCreator
    public
    PendingState (@JsonProperty("pending") final boolean pending,
                  @JsonProperty("reconnecting") final boolean reconnecting) {
        this.pending = pending;
        this.reconnecting = reconnecting;
    }

    /**
     * Gets the pending state.
     *
     * @return the pending state
     */
    public synchronized
    boolean getPendingState () {
        return this.pending;
    }

    /**
     * Schwebezustand eines Klienten wird gesetzt. Der Wiederverbindungszustand wird
     * zurückgesetzt. Ein verbundener Klient (pending == false) kann nicht
     * wiederverbunden werden. Ein frisch schwebender (pending == true) kann nicht
     * gleichzeitig wiederverbunden werden.
     *
     * @param pendingState the new pending state
     */
    public synchronized
    void setPendingState (final boolean pendingState) {
        this.pending = pendingState;
    }

    /**
     * Gets the reconnecting.
     *
     * @return the reconnecting
     */
    public synchronized
    boolean getReconnecting () {
        return this.reconnecting;
    }

    /**
     * Can only be set true if pending is also set to true.
     *
     * @return true, if successful
     */
    public synchronized
    boolean setReconnecting (boolean reconnectState) {
        final var reconnectSet = true;

        if (reconnectState && this.pending && !this.reconnecting) {
            this.reconnecting = true;
            return reconnectSet;
        } else if (!reconnectState) {
            this.reconnecting = false;
            return reconnectSet;
        }
        return !reconnectSet;
    }
}
