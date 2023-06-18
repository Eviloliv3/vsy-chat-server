/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package de.vsy.chat.server.two_server_test.chat;

import de.vsy.chat.server.raw_server_test.ServerPortProvider;
import de.vsy.chat.server.raw_server_test.TestClientDataProvider;
import de.vsy.chat.server.raw_server_test.chat.TestClientMessage;
import org.apache.logging.log4j.ThreadContext;

import static de.vsy.shared_utility.standard_value.ThreadContextValues.LOG_FILE_CONTEXT_KEY;

/**
 *
 */
public class ClientMessageBehaviour extends TestClientMessage {

    public ClientMessageBehaviour() {
        super(ServerPortProvider.DUAL_SERVER_PORT_PROVIDER, TestClientDataProvider.CHAT_CLIENT_LIST);
        ThreadContext.put(LOG_FILE_CONTEXT_KEY, "dualServerMessage");
    }
}
