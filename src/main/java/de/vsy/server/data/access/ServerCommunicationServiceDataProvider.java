
package de.vsy.server.data.access;

import de.vsy.server.data.SocketConnectionDataManager;
import de.vsy.server.persistent_data.server_data.CommunicatorPersistenceDAO;
import de.vsy.server.persistent_data.server_data.temporal.LiveClientStateDAO;
import de.vsy.server.service.ServicePacketBufferManager;

/**
 * Provides appropriate server data access for inter server communication services.
 */
public interface ServerCommunicationServiceDataProvider extends ServiceBaseDataProvider {

    LiveClientStateDAO getLiveClientStateDAO();

    SocketConnectionDataManager getServerConnectionDataManager();

    ServicePacketBufferManager getServicePacketBufferManager();

    CommunicatorPersistenceDAO getCommunicatorDataAccessor();

}
