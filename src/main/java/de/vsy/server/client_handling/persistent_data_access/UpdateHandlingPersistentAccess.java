package de.vsy.server.client_handling.persistent_data_access;

import de.vsy.server.persistent_data.client_data.ContactListDAO;
import de.vsy.server.persistent_data.client_data.MessageDAO;

/**
 * The Interface UpdateHandlingPersistentAccess.
 */
public interface UpdateHandlingPersistentAccess {

  /**
   * Gets the contactlist accessLimiter provider.
   *
   * @return the contactlist accessLimiter provider
   */
  ContactListDAO getContactlistDAO();

  /**
   * Gets the ${e.g(1).rsfl()}.
   *
   * @return the client persistant dataManagement accessLimiter
   */
  MessageDAO getMessageDAO();
}
