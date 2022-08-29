package de.vsy.chat.server.client_handling.data_management.logic;

import de.vsy.chat.server.persistent_data.data_bean.CommunicatorData;

public
interface AuthenticationStateControl extends ClientStateControl {

    /**
     * Zuerst werden die Klientendaten für den authentifizierten Klienten
     * eingetragen. Danach wird der Zustand AUTHENTICATED hinzugefügt. Diese
     * Reihenfolge ist maßgeblich für den Erfolg der Benachrichtigung von
     * zustandsabhängigen Objekten, da diese ebenfalls von den Klientendaten abhängig
     * sein könnten.
     *
     * @param clientData die Klientendaten
     */
    boolean loginClient (CommunicatorData clientData);

    boolean reconnectClient (CommunicatorData clientData);

    /** Entfernt alle Daten, die den Klienten als authentifiziert auszeichnen. */
    void logoutClient ();

    boolean changePendingState (boolean isPending);

    boolean getPendingState ();

    boolean changeReconnectionState (boolean newState);

    boolean getReconnectionState ();
}
