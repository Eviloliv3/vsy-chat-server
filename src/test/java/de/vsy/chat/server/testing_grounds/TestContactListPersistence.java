package de.vsy.chat.server.testing_grounds;

import static de.vsy.chat.server.raw_server_test.TestClientDataProvider.ADRIAN_1_COMM;
import static de.vsy.shared_transmission.packet.content.relation.EligibleContactEntity.CLIENT;
import static de.vsy.shared_transmission.packet.content.relation.EligibleContactEntity.CLIENT;

import de.vsy.server.persistent_data.client_data.ContactListDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestContactListPersistence {

  final ContactListDAO contactlist = new ContactListDAO();

  @BeforeEach
  public void initContactListAccess() throws InterruptedException {
    contactlist.createFileAccess(15000);
  }

  @AfterEach
  public void cutContactListAccess() {
    contactlist.removeFileAccess();
  }

  @Test
  void addContact() {
    Assertions.assertTrue(contactlist.addContact(CLIENT, ADRIAN_1_COMM.getCommunicatorId()));
  }
}
