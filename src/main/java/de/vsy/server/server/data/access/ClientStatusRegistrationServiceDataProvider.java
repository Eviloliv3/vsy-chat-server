/*
 *
 */
package de.vsy.server.server.data.access;

import de.vsy.server.service.ServicePacketBufferManager;
import de.vsy.server.server.server_connection.ServerConnectionDataManager;

public
interface ClientStatusRegistrationServiceDataProvider
        extends ServiceBaseDataProvider {

    /**
     * Gets the server connection dataManagement.
     *
     * @return the server connection dataManagement
     */
    ServerConnectionDataManager getServerConnectionDataManager ();

    /**
     * Gets the service PacketBuffer manager.
     *
     * @return the service PacketBuffer manager
     */
    ServicePacketBufferManager getServicePacketBufferManager ();
}
