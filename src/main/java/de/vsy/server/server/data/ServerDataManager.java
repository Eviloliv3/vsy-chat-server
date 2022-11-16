/*
 *
 */
package de.vsy.server.server.data;

import de.vsy.server.server.data.socketConnection.LocalServerConnectionData;
import de.vsy.server.service.ServicePacketBufferManager;

/**
 * Basisverwaltungseinheit aller serverbezogenen Daten.
 */
public class ServerDataManager {

  private final ServerSynchronizationManager serverSynchronization;
  private final SocketConnectionDataManager serverNodeManager;
  private final AbstractPacketCategorySubscriptionManager clientSubscriptionManager;
  private final AbstractPacketCategorySubscriptionManager serviceSubscriptionManager;
  private final ServicePacketBufferManager servicePacketBufferManager;

  /**
   * Instantiates a new server dataManagement manager.
   */
  public ServerDataManager(final LocalServerConnectionData localServerConnectionData) {
    this.clientSubscriptionManager = new ClientSubscriptionManager();
    this.serviceSubscriptionManager = new ServiceSubscriptionManager();
    this.servicePacketBufferManager = new ServicePacketBufferManager();
    this.serverNodeManager = new SocketConnectionDataManager(localServerConnectionData);
    this.serverSynchronization = new ServerSynchronizationManager(this.serverNodeManager);
  }

  public SocketConnectionDataManager getServerConnectionDataManager() {
    return this.serverNodeManager;
  }

  public ServicePacketBufferManager getServicePacketBufferManager() {
    return this.servicePacketBufferManager;
  }

  public AbstractPacketCategorySubscriptionManager getClientCategorySubscriptionManager() {
    return this.clientSubscriptionManager;
  }

  public AbstractPacketCategorySubscriptionManager getServiceSubscriptionManager() {
    return this.serviceSubscriptionManager;
  }

  public ServerSynchronizationManager getServerSynchronizationManager() {
    return this.serverSynchronization;
  }
}
