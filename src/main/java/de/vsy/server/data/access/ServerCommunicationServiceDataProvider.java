/*
 *
 */
package de.vsy.server.data.access;

import de.vsy.server.data.SocketConnectionDataManager;
import de.vsy.server.persistent_data.server_data.CommunicatorPersistenceDAO;
import de.vsy.server.persistent_data.server_data.temporal.LiveClientStateDAO;
import de.vsy.server.service.ServicePacketBufferManager;

/**
 * Ensures all dataManagement accessLimiter needed by InterServerCommunicationServices is provided.
 */
public interface ServerCommunicationServiceDataProvider extends ServiceBaseDataProvider {

    /**
     * Returns the client state PersistenceDAO provider.
     *
     * @return the client state PersistenceDAO provider
     */
    LiveClientStateDAO getLiveClientStateDAO();

    /**
     * Returns the server connection dataManagement.
     *
     * @return the server connection dataManagement
     */
    SocketConnectionDataManager getServerConnectionDataManager();

    ServicePacketBufferManager getServicePacketBufferManager();

    CommunicatorPersistenceDAO getCommunicatorDataAccessor();

}
