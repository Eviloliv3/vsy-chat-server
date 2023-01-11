package de.vsy.chat.server.two_server_test.authentication;

import de.vsy.chat.server.raw_server_test.ServerPortProvider;
import de.vsy.chat.server.raw_server_test.authentication.TestAccountDeletionBehaviour;

public class AccountDeletionBehaviour extends TestAccountDeletionBehaviour {
    public AccountDeletionBehaviour() {
        super(ServerPortProvider.DUAL_SERVER_PORT_PROVIDER, null);
    }
}
