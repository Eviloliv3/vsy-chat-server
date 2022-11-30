package de.vsy.server.client_handling.persistent_data_access;

import de.vsy.server.persistent_data.client_data.ContactListDAO;
import de.vsy.server.persistent_data.client_data.MessageDAO;

/**
 * The Interface UpdateHandlingPersistentAccess.
 */
public interface UpdateHandlingPersistentAccess {

  /**
   * Returns the contact list access provider.
   *
   * @return ContactListDAO
   */
  ContactListDAO getContactListDAO();

  /**
   * Returns the Message access provider.
   *
   * @return MessageDAO
   */
  MessageDAO getMessageDAO();
}
