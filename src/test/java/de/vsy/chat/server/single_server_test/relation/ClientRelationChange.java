/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package de.vsy.chat.server.single_server_test.relation;

import static de.vsy.shared_utility.standard_value.ThreadContextValues.LOG_FILE_CONTEXT_KEY;

import de.vsy.chat.server.raw_server_test.ServerPortProvider;
import de.vsy.chat.server.raw_server_test.TestClientDataProvider;
import de.vsy.chat.server.raw_server_test.relation.TestClientRelationChanges;
import org.apache.logging.log4j.ThreadContext;

/**
 *
 */
public class ClientRelationChange extends TestClientRelationChanges {

  public ClientRelationChange() {
    super(ServerPortProvider.SINGLE_SERVER_PORT_PROVIDER, TestClientDataProvider.RELA_CLIENT_LIST);
    ThreadContext.put(LOG_FILE_CONTEXT_KEY, "singleServerRelation");
  }
}
