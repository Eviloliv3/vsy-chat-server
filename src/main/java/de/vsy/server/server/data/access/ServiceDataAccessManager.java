package de.vsy.server.server.data.access;

import de.vsy.server.persistent_data.server_data.CommunicatorPersistenceDAO;
import de.vsy.server.persistent_data.server_data.temporal.LiveClientStateDAO;
import de.vsy.server.server.data.AbstractPacketCategorySubscriptionManager;
import de.vsy.server.server.data.ServerDataManager;
import de.vsy.server.server.data.ServerPersistentDataManager;
import de.vsy.server.server.server_connection.LocalServerConnectionData;
import de.vsy.server.server.server_connection.ServerConnectionDataManager;
import de.vsy.server.service.ServicePacketBufferManager;

/** The Class ServiceDataAccessManager. */
public class ServiceDataAccessManager implements ClientStatusRegistrationServiceDataProvider,
		PacketAssignmentServiceDataProvider, ServerCommunicationServiceDataProvider, ErrorHandlingServiceDataProvider {

	private final CommunicatorPersistenceDAO clientRegistry;
	private final LiveClientStateDAO persistenClientStates;
	private final AbstractPacketCategorySubscriptionManager clientSubscriptionManager;
	private final ServerConnectionDataManager serverConnectionDataManager;
	private final AbstractPacketCategorySubscriptionManager serverSubscriptionManager;
	private final ServicePacketBufferManager serviceBuffers;

	/**
	 * Instantiates a new registration dataManagement manager.
	 *
	 * @param serverDataAccess            the failed services
	 * @param serverPersistentDataManager the server persistent data manager
	 */
	public ServiceDataAccessManager(final ServerDataManager serverDataAccess,
			final ServerPersistentDataManager serverPersistentDataManager) {
		this.clientRegistry = serverPersistentDataManager.getCommunicationEntityAccessManager();
		this.persistenClientStates = serverPersistentDataManager.getClientStateAccessManager();
		this.clientSubscriptionManager = serverDataAccess.getClientCategorySubscriptionManager();
		this.serverConnectionDataManager = serverDataAccess.getServerConnectionDataManager();
		this.serverSubscriptionManager = serverDataAccess.getServiceSubscriptionManager();
		this.serviceBuffers = serverDataAccess.getServicePacketBufferManager();
	}

	@Override
	public CommunicatorPersistenceDAO getCommunicatorDataAccessor() {
		return this.clientRegistry;
	}

	@Override
	public LocalServerConnectionData getLocalServerNodeData() {
		return this.serverConnectionDataManager.getLocalServerConnectionData();
	}

	@Override
	public ServerConnectionDataManager getServerConnectionDataManager() {
		return this.serverConnectionDataManager;
	}

	@Override
	public ServicePacketBufferManager getServicePacketBufferManager() {
		return this.serviceBuffers;
	}

	@Override
	public LiveClientStateDAO getLiveClientStateDAO() {
		return this.persistenClientStates;
	}

	@Override
	public AbstractPacketCategorySubscriptionManager getClientSubscriptionManager() {
		return this.clientSubscriptionManager;
	}

	@Override
	public AbstractPacketCategorySubscriptionManager getServiceSubscriptionManager() {
		return this.serverSubscriptionManager;
	}

	@Override
	public LocalServerConnectionData getLocalServerConnectionData() {
		return this.serverConnectionDataManager.getLocalServerConnectionData();
	}
}
