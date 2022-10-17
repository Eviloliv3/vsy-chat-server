package de.vsy.server.client_handling.data_management.logic;

import de.vsy.server.server.client_management.ClientState;

public interface ClientStateControl {

  ClientState getPersistentClientState();

  boolean changeClientState(final ClientState clientState, final boolean changeTo);

  boolean changePersistentClientState(ClientState clientState, final boolean changeTo);
}
