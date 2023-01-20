
package de.vsy.server.data.access;

import de.vsy.server.data.SocketConnectionDataManager;
import de.vsy.server.persistent_data.server_data.temporal.LiveClientStateDAO;
import de.vsy.server.service.ServicePacketBufferManager;

/**
 * Provides appropriate server data access for ClientState synchronization services.
 */
public interface ClientStatusRegistrationServiceDataProvider extends ServiceBaseDataProvider {

    SocketConnectionDataManager getServerConnectionDataManager();

    ServicePacketBufferManager getServicePacketBufferManager();

    LiveClientStateDAO getLiveClientStateDAO();
}
