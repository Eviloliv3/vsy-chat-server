package de.vsy.chat.server.two_server_test.status;

import de.vsy.chat.server.raw_server_test.ServerPortProvider;
import de.vsy.chat.server.raw_server_test.TestClientDataProvider;
import de.vsy.chat.server.raw_server_test.status.TestStatusChange;
import org.apache.logging.log4j.ThreadContext;

import static de.vsy.shared_utility.standard_value.ThreadContextValues.LOG_FILE_CONTEXT_KEY;

class ClientStatusChangeRelatedBehaviour extends TestStatusChange {

    public ClientStatusChangeRelatedBehaviour() {
        super(ServerPortProvider.DUAL_SERVER_PORT_PROVIDER, TestClientDataProvider.STATUS_CLIENT_LIST);
        ThreadContext.put(LOG_FILE_CONTEXT_KEY, "dualServerStatus");
    }
}
