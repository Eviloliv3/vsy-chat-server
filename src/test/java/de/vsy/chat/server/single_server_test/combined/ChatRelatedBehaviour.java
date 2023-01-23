package de.vsy.chat.server.single_server_test.combined;

import de.vsy.chat.server.raw_server_test.ServerPortProvider;
import de.vsy.chat.server.raw_server_test.TestClientDataProvider;
import de.vsy.chat.server.raw_server_test.combined.TestChatRelated;
import org.apache.logging.log4j.ThreadContext;

import static de.vsy.shared_utility.standard_value.ThreadContextValues.LOG_FILE_CONTEXT_KEY;

public class ChatRelatedBehaviour extends TestChatRelated {
    public ChatRelatedBehaviour() {
        super(ServerPortProvider.SINGLE_SERVER_PORT_PROVIDER, TestClientDataProvider.CHAT_CLIENT_LIST);
        ThreadContext.put(LOG_FILE_CONTEXT_KEY, "singleServerChatRelated");
    }
}
