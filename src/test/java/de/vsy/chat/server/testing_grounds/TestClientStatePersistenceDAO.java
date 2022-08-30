/**  */
package de.vsy.chat.server.testing_grounds;

import de.vsy.server.persistent_data.server_data.temporal.LiveClientStateDAO;
import de.vsy.server.server.client_management.ClientState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_SERVER_ID;

/** @author Frederic Heath */
class TestClientStatePersistenceDAO {

    final LiveClientStateDAO statePersist = new LiveClientStateDAO();

    @BeforeEach
    void createAccess ()
    throws InterruptedException {
        statePersist.createFileAccess();
    }

    @AfterEach
    void removeAllClients () {
        statePersist.removeAllClientStates();
        statePersist.removeFileAccess();
    }

    @Test
    void addAuthenticated () {
        statePersist.changeClientState(STANDARD_SERVER_ID, 132456,
                                       ClientState.AUTHENTICATED);
        Assertions.assertEquals(ClientState.AUTHENTICATED,
                                statePersist.getClientState(132456)
                                            .getCurrentState());
    }

    @Test
    void removeAuthenticated () {
        statePersist.changeClientState(STANDARD_SERVER_ID, 132456,
                                       ClientState.AUTHENTICATED);
        statePersist.removeClientState(132456);
        Assertions.assertNull(statePersist.getClientState(132456));
    }

    @Test
    void checkPendingState () {
        statePersist.changeClientState(STANDARD_SERVER_ID, 132456,
                                       ClientState.AUTHENTICATED);
        statePersist.changeClientPendingState(132456, true);
        Assertions.assertTrue(statePersist.getClientPendingState(132456));
    }

    @Test
    void checkIllegalReconnectState () {
        statePersist.changeClientState(STANDARD_SERVER_ID, 132456,
                                       ClientState.AUTHENTICATED);
        statePersist.changeReconnectionState(132456, true);
        Assertions.assertFalse(statePersist.getClientPendingState(132456));
    }

    @Test
    void checkLegalReconnectState () {
        statePersist.changeClientState(STANDARD_SERVER_ID, 132456,
                                       ClientState.AUTHENTICATED);
        statePersist.changeClientPendingState(132456, true);
        Assertions.assertTrue(statePersist.changeReconnectionState(132456, true));
    }
}
