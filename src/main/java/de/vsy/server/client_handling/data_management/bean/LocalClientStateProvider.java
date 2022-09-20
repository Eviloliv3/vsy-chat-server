package de.vsy.server.client_handling.data_management.bean;

import de.vsy.server.server.client_management.ClientState;

public
interface LocalClientStateProvider {

    boolean checkClientState (ClientState toCheck);

    /**
     * Gibt einmal aus, ob sich der Klientenzustand veraendert hat.
     *
     * @return true, wenn sich der Klientenzustand veraendert hat, false wenn sich
     * der Klientenzustand nicht veraendert hat ODER die Veraenderung schon einmal
     * abgefragt wurde.
     */
    boolean clientStateHasChanged ();
}
