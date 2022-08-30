package de.vsy.server.client_handling.persistent_data_access;

import de.vsy.server.persistent_data.client_data.ContactListDAO;
import de.vsy.server.persistent_data.client_data.MessageDAO;

/** The Interface ChatHandlingPersistentAccess. */
public
interface ChatHandlingPersistentAccess {

    /**
     * Gets the contactlist accessLimiter provider.
     *
     * @return the contactlist accessLimiter provider
     */
    ContactListDAO getContactlistDAO ();

    /**
     * Gets the message accessLimiter provider.
     *
     * @return the message accessLimiter provider
     */
    MessageDAO getMessageDAO ();
}
