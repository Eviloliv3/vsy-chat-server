package de.vsy.server.persistent_data.manipulation_utility;

import de.vsy.server.persistent_data.client_data.ContactListDAO;
import de.vsy.server.persistent_data.client_data.MessageDAO;
import de.vsy.shared_transmission.packet.content.relation.EligibleContactEntity;

public class RelationManipulator {

  private RelationManipulator() {
  }

  public static void addContact(EligibleContactEntity contactType, final int contactId,
      final ContactListDAO contactListAccess) {
    contactListAccess.addContact(contactType, contactId);
  }

  public static void removeContact(EligibleContactEntity contactType, final int contactId,
      final ContactListDAO contactListAccess, final MessageDAO messageHistoryAccess) {
    contactListAccess.removeContactFromSet(contactType, contactId);
    messageHistoryAccess.removeMessages(contactId);
  }
}
