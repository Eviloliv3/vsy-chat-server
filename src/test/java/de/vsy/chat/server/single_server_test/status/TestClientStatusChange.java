package de.vsy.chat.server.single_server_test.status;

import de.vsy.chat.server.raw_server_test.ServerPortProvider;
import de.vsy.chat.server.raw_server_test.TestClientDataProvider;
import de.vsy.chat.server.raw_server_test.status.TestStatusChange;
import org.apache.logging.log4j.ThreadContext;

class TestClientStatusChange extends TestStatusChange {

    public
    TestClientStatusChange () {
        super(ServerPortProvider.SINGLE_SERVER_PORT_PROVIDER,
              TestClientDataProvider.STATUS_CLIENT_LIST);
        ThreadContext.put("logFilename", "singleServerStatus");
    }
}
