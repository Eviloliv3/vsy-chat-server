package de.vsy.server.data.access;

import de.vsy.server.data.socketConnection.LocalServerConnectionData;
import de.vsy.server.service.ServicePacketBufferManager;

/**
 * The Interface PacketAssignmentServiceDataProvider.
 */
public interface PacketAssignmentServiceDataProvider extends ServiceBaseDataProvider {

  /**
   * Returns the server connection dataManagement.
   *
   * @return the server connection dataManagement
   */
  LocalServerConnectionData getLocalServerNodeData();

  /**
   * Returns the service PacketBuffer manager.
   *
   * @return the service PacketBuffer manager
   */
  ServicePacketBufferManager getServicePacketBufferManager();
}
