package de.vsy.chat.server.client_handling.data_management.bean;

import de.vsy.chat.server.server.client_management.ClientState;

public
interface LocalClientStateProvider {

    boolean checkClientState (ClientState toCheck);

    boolean clientStateHasRisen ();

    boolean clientStateHasChanged ();
}
