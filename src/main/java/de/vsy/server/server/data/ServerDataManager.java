/*
 *
 */
package de.vsy.server.server.data;

import de.vsy.server.server.server_connection.LocalServerConnectionData;
import de.vsy.server.server.server_connection.ServerConnectionDataManager;
import de.vsy.server.service.ServicePacketBufferManager;

/** Basisverwaltungseinheit aller serverbezogenen Daten. */
public class ServerDataManager {

	private final ServerConnectionDataManager serverNodeManager;
	private final AbstractPacketCategorySubscriptionManager clientSubscriptionManager;
	private final AbstractPacketCategorySubscriptionManager serviceSubscriptionManager;
	private final ServicePacketBufferManager servicePacketBufferManager;

	/** Instantiates a new server dataManagement manager. */
	public ServerDataManager(final LocalServerConnectionData localServerConnectionData) {
		this.serverNodeManager = new ServerConnectionDataManager(localServerConnectionData);
		this.clientSubscriptionManager = new ClientSubscriptionManager();
		this.serviceSubscriptionManager = new ServiceSubscriptionManager();
		this.servicePacketBufferManager = new ServicePacketBufferManager();
	}

	public ServerConnectionDataManager getServerConnectionDataManager() {
		return this.serverNodeManager;
	}

	public ServicePacketBufferManager getServicePacketBufferManager() {
		return this.servicePacketBufferManager;
	}

	public AbstractPacketCategorySubscriptionManager getClientCategorySubscriptionManager() {
		return this.clientSubscriptionManager;
	}

	public AbstractPacketCategorySubscriptionManager getServiceSubscriptionManager() {
		return this.serviceSubscriptionManager;
	}
}
