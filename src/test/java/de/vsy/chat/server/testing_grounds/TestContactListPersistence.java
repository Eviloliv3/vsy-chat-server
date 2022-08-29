package de.vsy.chat.server.testing_grounds;

import de.vsy.chat.server.persistent_data.client_data.ContactListDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static de.vsy.chat.shared_transmission.packet.content.relation.EligibleContactEntity.CLIENT;

class TestContactListPersistence {

    final ContactListDAO contactlist = new ContactListDAO();

    @BeforeEach
    public
    void initContactListAccess ()
    throws InterruptedException {
        contactlist.createFileAccess(15000);
    }

    @AfterEach
    public
    void cutContactListAccess () {
        contactlist.removeFileAccess();
    }

    @Test
    void addContact () {
        Assertions.assertTrue(contactlist.addContact(CLIENT, 15003));
    }
}
