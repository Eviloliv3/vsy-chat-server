package de.vsy.server.client_handling.persistent_data_access;

import de.vsy.server.persistent_data.client_data.ContactListDAO;
import de.vsy.server.persistent_data.client_data.MessageDAO;

public interface RelationHandlingPersistenceAccess {

    /**
     * Returns the contact list access provider.
     *
     * @return the contactlist accessLimiter provider
     */
    ContactListDAO getContactListDAO();

    /**
     * Returns the message access provider.
     *
     * @return MessageDAO
     */
    MessageDAO getMessageDAO();
}
