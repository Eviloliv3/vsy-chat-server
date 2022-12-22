/**
 *
 */
package de.vsy.chat.server.testing_grounds;

import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_SERVER_ID;

import de.vsy.server.client_management.ClientState;
import de.vsy.server.data.SocketConnectionDataManager;
import de.vsy.server.data.socketConnection.LocalServerConnectionData;
import de.vsy.server.persistent_data.server_data.temporal.LiveClientStateDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 */
class TestClientStatePersistenceDAO {

  final LiveClientStateDAO statePersist = new LiveClientStateDAO(new SocketConnectionDataManager(
      LocalServerConnectionData.valueOf(-1, null)));

  @BeforeEach
  void createAccess() throws InterruptedException {
    statePersist.createFileAccess();
  }

  @AfterEach
  void removeAllClients() {
    statePersist.removeAllClientStates();
    statePersist.removeFileAccess();
  }

  @Test
  void addAuthenticated() {
    statePersist.changeClientState(STANDARD_SERVER_ID, 132456, ClientState.AUTHENTICATED);
    Assertions.assertEquals(ClientState.AUTHENTICATED,
        statePersist.getClientState(132456).getCurrentState());
  }

  @Test
  void removeAuthenticated() {
    statePersist.changeClientState(STANDARD_SERVER_ID, 132456, ClientState.AUTHENTICATED);
    statePersist.removeClientState(132456);
    Assertions.assertNull(statePersist.getClientState(132456));
  }

  @Test
  void checkPendingState() {
    statePersist.changeClientState(STANDARD_SERVER_ID, 132456, ClientState.AUTHENTICATED);
    statePersist.changeClientPendingState(132456, true);
    Assertions.assertTrue(statePersist.getClientPendingState(132456));
  }

  @Test
  void checkIllegalReconnectState() {
    statePersist.changeClientState(STANDARD_SERVER_ID, 132456, ClientState.AUTHENTICATED);
    statePersist.changeReconnectionState(132456, true);
    Assertions.assertFalse(statePersist.getClientPendingState(132456));
  }

  @Test
  void checkLegalReconnectState() {
    statePersist.changeClientState(STANDARD_SERVER_ID, 132456, ClientState.AUTHENTICATED);
    statePersist.changeClientPendingState(132456, true);
    Assertions.assertTrue(statePersist.changeReconnectionState(132456, true));
  }
}
