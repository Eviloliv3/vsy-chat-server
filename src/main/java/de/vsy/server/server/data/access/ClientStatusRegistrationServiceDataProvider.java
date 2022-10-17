/*
 *
 */
package de.vsy.server.server.data.access;

import de.vsy.server.persistent_data.server_data.temporal.LiveClientStateDAO;
import de.vsy.server.server.server_connection.ServerConnectionDataManager;
import de.vsy.server.service.ServicePacketBufferManager;

public interface ClientStatusRegistrationServiceDataProvider extends ServiceBaseDataProvider {

	/**
	 * Gets the server connection dataManagement.
	 *
	 * @return the server connection dataManagement
	 */
	ServerConnectionDataManager getServerConnectionDataManager();

	/**
	 * Gets the service PacketBuffer manager.
	 *
	 * @return the service PacketBuffer manager
	 */
	ServicePacketBufferManager getServicePacketBufferManager();

	LiveClientStateDAO getLiveClientStateDAO();
}
