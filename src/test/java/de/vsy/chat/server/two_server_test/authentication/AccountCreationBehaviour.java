/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package de.vsy.chat.server.two_server_test.authentication;

import static de.vsy.shared_utility.standard_value.ThreadContextValues.LOG_FILE_CONTEXT_KEY;

import org.apache.logging.log4j.ThreadContext;

import de.vsy.chat.server.raw_server_test.ServerPortProvider;
import de.vsy.chat.server.raw_server_test.TestClientDataProvider;
import de.vsy.chat.server.raw_server_test.authentication.TestAccountCreationBehaviour;

/** @author fredward */
public class AccountCreationBehaviour extends TestAccountCreationBehaviour {

	public AccountCreationBehaviour() {
		super(ServerPortProvider.DUAL_SERVER_PORT_PROVIDER, TestClientDataProvider.AUTH_CLIENT_LIST);
		ThreadContext.put(LOG_FILE_CONTEXT_KEY, "dualServerAccCreation");
	}
}
