package de.vsy.server.client_handling.data_management.logic;

import de.vsy.server.client_management.ClientState;
import de.vsy.server.persistent_data.data_bean.CommunicatorData;

public interface AuthenticationStateControl extends ClientStateControl {

  /**
   * Firstly adds the authenticated client's communicator data to local state cache. Then adds
   * AUTHENTICATED state. This order should be followed, because other objects may observe state
   * changes and subsequently use the communicator data.
   * @param clientData CommunicatorData
   */
  boolean loginClient(CommunicatorData clientData);

  ClientState reconnectClient(CommunicatorData clientData);

  /**
   * Removes all client specific data and states.
   */
  void logoutClient();

  /**
   * Changes the local client's global pending state.
   * @param isPending boolean
   * @return true if global pending state could be change; false otherwise
   */
  boolean changePendingState(boolean isPending);

  /**
   * Returns the client's global pending state.
   * @return boolean
   */
  boolean getPendingState();

  /**
   * Tries to change the local client's global reconnection state.
   * @param newState boolean
   * @return boolean
   */
  boolean changeReconnectionState(boolean newState);

  /**
   * Returns the local client's global reconnection state.
   * @return boolean
   */
  boolean getReconnectionState();
}
