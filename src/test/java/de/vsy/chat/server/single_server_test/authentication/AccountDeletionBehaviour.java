package de.vsy.chat.server.single_server_test.authentication;

import de.vsy.chat.server.raw_server_test.ServerPortProvider;
import de.vsy.chat.server.raw_server_test.TestClientDataProvider;
import de.vsy.chat.server.raw_server_test.authentication.TestAccountDeletionBehaviour;

public class AccountDeletionBehaviour extends TestAccountDeletionBehaviour {
    public AccountDeletionBehaviour() {
        super(ServerPortProvider.SINGLE_SERVER_PORT_PROVIDER, null);
    }
}
