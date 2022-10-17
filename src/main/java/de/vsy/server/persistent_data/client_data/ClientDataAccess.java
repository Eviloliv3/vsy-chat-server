package de.vsy.server.persistent_data.client_data;

import de.vsy.server.persistent_data.PersistentDataAccess;

/**
 * Aktiviert spezialisierten Dateizugriff auf Clientdaten zur Laufzeit.
 */
public interface ClientDataAccess extends PersistentDataAccess {

  /**
   * Creates the file accessLimiter.
   *
   * @param clientId the client id
   */
  void createFileAccess(int clientId) throws InterruptedException;
}
