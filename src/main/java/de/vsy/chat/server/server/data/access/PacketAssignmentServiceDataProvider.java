package de.vsy.chat.server.server.data.access;

import de.vsy.chat.server.server.server_connection.LocalServerConnectionData;
import de.vsy.chat.server.service.ServicePacketBufferManager;

/** The Interface PacketAssignmentServiceDataProvider. */
public
interface PacketAssignmentServiceDataProvider extends ServiceBaseDataProvider {

    /**
     * Gets the server connection dataManagement.
     *
     * @return the server connection dataManagement
     */
    LocalServerConnectionData getLocalServerNodeData ();

    /**
     * Gets the service PacketBuffer manager.
     *
     * @return the service PacketBuffer manager
     */
    ServicePacketBufferManager getServicePacketBufferManager ();
}
