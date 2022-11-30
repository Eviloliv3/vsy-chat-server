package de.vsy.server.data.access;

import de.vsy.server.data.AbstractPacketCategorySubscriptionManager;
import de.vsy.server.data.ServerSynchronizationManager;
import de.vsy.server.data.socketConnection.LocalServerConnectionData;

public interface ServiceBaseDataProvider {

  /**
   * Returns the client subscription manager.
   *
   * @return the client subscription manager
   */
  AbstractPacketCategorySubscriptionManager getClientSubscriptionManager();

  /**
   * Returns the server subscription manager.
   *
   * @return the server subscription manager
   */
  AbstractPacketCategorySubscriptionManager getServiceSubscriptionManager();

  /**
   * Returns the local server connection data
   *
   * @return the local server connection data
   */
  LocalServerConnectionData getLocalServerConnectionData();

  /**
   * Gets an object providing synchronization methods.
   *
   * @return the server synchronization manager
   */
  ServerSynchronizationManager getServerSynchronizationManager();
}
