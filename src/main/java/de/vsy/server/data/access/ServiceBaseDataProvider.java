package de.vsy.server.data.access;

import de.vsy.server.data.AbstractPacketCategorySubscriptionManager;
import de.vsy.server.data.ServerSynchronizationManager;
import de.vsy.server.data.socketConnection.LocalServerConnectionData;

public interface ServiceBaseDataProvider {

  /**
   * Gets the client subscription manager.
   *
   * @return the client subscription manager
   */
  AbstractPacketCategorySubscriptionManager getClientSubscriptionManager();

  /**
   * Gets the server subscription manager.
   *
   * @return the server subscription manager
   */
  AbstractPacketCategorySubscriptionManager getServiceSubscriptionManager();

  /**
   * Gets the local server connection data
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
