/*
 *
 */
package de.vsy.server.client_handling.data_management.bean;

import de.vsy.server.server.client_management.ClientState;

/** Listener interface for clientState events. */
public
interface ClientStateListener {

    /**
     * Wertet den Zustand, im Zusammenhang mit der Information, ob er hinzugefügt
     * oder entfernt wurde, aus.
     *
     * @param changedState der veränderte Zustand
     * @param added Zustand neu oder entfernt
     */
    void evaluateNewState (ClientState changedState, boolean added);
}
