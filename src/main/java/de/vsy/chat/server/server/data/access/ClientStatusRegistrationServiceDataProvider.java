/*
 *
 */
package de.vsy.chat.server.server.data.access;

import de.vsy.chat.server.server.server_connection.ServerConnectionDataManager;
import de.vsy.chat.server.service.ServicePacketBufferManager;

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
