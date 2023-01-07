package de.vsy.chat.server.two_server_test;

import de.vsy.chat.server.raw_server_test.ServerPortProvider;
import de.vsy.chat.server.raw_server_test.TestClientDataProvider;
import de.vsy.chat.server.raw_server_test.status.TestStatusChange;

class TestClientStatusChange extends TestStatusChange {

    public TestClientStatusChange() {
        super(ServerPortProvider.DUAL_SERVER_PORT_PROVIDER, TestClientDataProvider.STATUS_CLIENT_LIST);
    }
}
