package de.vsy.server.persistent_data.manipulation_utility;

import de.vsy.shared_module.shared_module.data_element_validation.IdCheck;
import de.vsy.server.persistent_data.client_data.ContactListDAO;
import de.vsy.server.persistent_data.client_data.MessageDAO;
import de.vsy.shared_transmission.shared_transmission.packet.content.relation.EligibleContactEntity;

public
class RelationManipulator {

    private
    RelationManipulator () {}

    public static
    void addContact (EligibleContactEntity contactType, final int contactId,
                     final ContactListDAO contactListAccess) {
        if (contactType == null || IdCheck.checkData(contactId) != null ||
            contactListAccess == null) {
            final String errorMessage =
                    "Einer der Parameter ist ungueltig: Typ/" + contactType +
                    ", ID/" + contactId + ", Listenzugriff/" + contactListAccess;
            throw new IllegalArgumentException(errorMessage);
        }
        contactListAccess.addContact(contactType, contactId);
    }

    public static
    void removeContact (EligibleContactEntity contactType, final int contactId,
                        final ContactListDAO contactListAccess,
                        final MessageDAO messageHistoryAccess) {
        if (contactType == null || IdCheck.checkData(contactId) != null ||
            contactListAccess == null || messageHistoryAccess == null) {
            final String errorMessage =
                    "Einer der Parameter ist ungueltig: Typ/" + contactType +
                    ", ID/" + contactId + ", Listenzugriff/" + contactListAccess +
                    ", Nachrichtenzugriff/" + messageHistoryAccess;
            throw new IllegalArgumentException(errorMessage);
        }
        contactListAccess.removeContactFromSet(contactType, contactId);
        messageHistoryAccess.removeMessages(contactId);
    }
}
