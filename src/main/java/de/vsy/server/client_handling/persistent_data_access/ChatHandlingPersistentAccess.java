package de.vsy.server.client_handling.persistent_data_access;

import de.vsy.server.persistent_data.client_data.ContactListDAO;
import de.vsy.server.persistent_data.client_data.MessageDAO;

/**
 * The Interface ChatHandlingPersistentAccess.
 */
public interface ChatHandlingPersistentAccess {

  /**
   * Returns the contact list accessLimiter provider.
   *
   * @return ContactListDAO
   */
  ContactListDAO getContactListDAO();

  /**
   * Returns the message accessLimiter provider.
   *
   * @return MessageDAO
   */
  MessageDAO getMessageDAO();
}
