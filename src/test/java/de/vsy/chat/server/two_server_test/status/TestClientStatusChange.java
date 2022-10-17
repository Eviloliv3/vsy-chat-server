package de.vsy.chat.server.two_server_test.status;

import static de.vsy.shared_utility.standard_value.ThreadContextValues.LOG_FILE_CONTEXT_KEY;

import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.TestInstance;

import de.vsy.chat.server.raw_server_test.ServerPortProvider;
import de.vsy.chat.server.raw_server_test.TestClientDataProvider;
import de.vsy.chat.server.raw_server_test.status.TestStatusChange;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestClientStatusChange extends TestStatusChange {

	public TestClientStatusChange() {
		super(ServerPortProvider.DUAL_SERVER_PORT_PROVIDER, TestClientDataProvider.STATUS_CLIENT_LIST);
		ThreadContext.put(LOG_FILE_CONTEXT_KEY, "dualServerStatus");
	}
}
