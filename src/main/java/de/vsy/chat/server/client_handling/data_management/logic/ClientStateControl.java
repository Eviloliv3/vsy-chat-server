package de.vsy.chat.server.client_handling.data_management.logic;

import de.vsy.chat.server.server.client_management.ClientState;

public
interface ClientStateControl {

    ClientState getPersistentClientState ();

    boolean changeClientState (final ClientState clientState,
                               final boolean changeTo);

    boolean changePersistentClientState (ClientState clientState,
                                         final boolean changeTo);
}
