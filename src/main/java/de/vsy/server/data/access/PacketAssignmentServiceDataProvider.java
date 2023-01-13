package de.vsy.server.data.access;

import de.vsy.server.data.socketConnection.LocalServerConnectionData;
import de.vsy.server.service.ServicePacketBufferManager;

/**
 * Provides appropriate server data access for Packet assignment services.
 */
public interface PacketAssignmentServiceDataProvider extends ServiceBaseDataProvider {

    LocalServerConnectionData getLocalServerNodeData();

    ServicePacketBufferManager getServicePacketBufferManager();
}
