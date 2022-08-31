/*
 *
 */
package de.vsy.server.server.data.access;

import de.vsy.server.persistent_data.server_data.CommunicatorPersistenceDAO;
import de.vsy.server.persistent_data.server_data.temporal.LiveClientStateDAO;
import de.vsy.server.server.server_connection.ServerConnectionDataManager;
import de.vsy.server.service.ServicePacketBufferManager;

/**
 * Ensures all dataManagement accessLimiter needed by
 * InterServerCommunicationServices is provided.
 */
public
interface ServerCommunicationServiceDataProvider extends ServiceBaseDataProvider {

    /**
     * Gets the client state PersistenceDAO provider.
     *
     * @return the client state PersistenceDAO provider
     */
    LiveClientStateDAO getClientStateDAO ();

    /**
     * Gets the server connection dataManagement.
     *
     * @return the server connection dataManagement
     */
    ServerConnectionDataManager getServerConnectionDataManager ();

    ServicePacketBufferManager getServicePacketBufferManager ();

    CommunicatorPersistenceDAO getCommunicatorDataAccessor ();
}
