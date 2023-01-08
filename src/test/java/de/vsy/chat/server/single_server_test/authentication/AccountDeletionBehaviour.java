package de.vsy.chat.server.single_server_test.authentication;

import de.vsy.chat.server.raw_server_test.ServerPortProvider;
import de.vsy.chat.server.raw_server_test.TestClientDataProvider;
import de.vsy.chat.server.raw_server_test.authentication.TestAccountDeletionBehaviour;
import org.apache.logging.log4j.ThreadContext;

import static de.vsy.shared_utility.standard_value.ThreadContextValues.LOG_FILE_CONTEXT_KEY;

public class AccountDeletionBehaviour extends TestAccountDeletionBehaviour {
    public AccountDeletionBehaviour() {
        super(ServerPortProvider.SINGLE_SERVER_PORT_PROVIDER, null);
        ThreadContext.put(LOG_FILE_CONTEXT_KEY, "singleServerDeletion");
    }
}
