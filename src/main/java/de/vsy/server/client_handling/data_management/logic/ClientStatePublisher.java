package de.vsy.server.client_handling.data_management.logic;

import static de.vsy.shared_transmission.shared_transmission.packet.property.communicator.CommunicationEndpoint.getServerEntity;
import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_SERVER_ID;
import static java.util.Set.of;

import de.vsy.server.client_handling.data_management.bean.ClientStateListener;
import de.vsy.server.client_handling.data_management.bean.LocalClientDataProvider;
import de.vsy.server.persistent_data.client_data.ContactListDAO;
import de.vsy.server.server.client_management.ClientState;
import de.vsy.server.server.server_connection.ServerConnectionDataManager;
import de.vsy.server.server_packet.content.builder.ExtendedStatusSyncBuilder;
import de.vsy.server.server_packet.content.builder.SimpleStatusSyncBuilder;
import de.vsy.server.server_packet.dispatching.PacketDispatcher;
import de.vsy.shared_module.shared_module.packet_creation.PacketCompiler;
import de.vsy.shared_transmission.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.shared_transmission.packet.content.relation.EligibleContactEntity;

public class ClientStatePublisher implements ClientStateListener {

	private static ServerConnectionDataManager serverConnectionNodes;
	private final LocalClientDataProvider clientDataManager;
	private final ContactListDAO contactListAccess;
	private final PacketDispatcher dispatcher;

	public ClientStatePublisher(final LocalClientDataProvider clientDataManager,
			final ContactListDAO contactListProvider, final PacketDispatcher dispatcher) {
		this.clientDataManager = clientDataManager;
		this.contactListAccess = contactListProvider;
		this.dispatcher = dispatcher;
	}

	public static void setupStaticServerDataAccess(final ServerConnectionDataManager serverConnectionDataManager) {
		serverConnectionNodes = serverConnectionDataManager;
	}

	@Override
	public void evaluateNewState(ClientState clientState, boolean changeTo) {
		final var statusPacket = createStatusSyncPacket(clientState, changeTo);

		if (statusPacket != null) {
			dispatcher.dispatchPacket(statusPacket);
		}
	}

	public Packet createStatusSyncPacket(ClientState clientState, boolean changeTo) {
		Packet statePacket;

		switch (clientState) {
		case AUTHENTICATED -> statePacket = buildSimpleStatusPacket(clientState, changeTo);
		case ACTIVE_MESSENGER -> statePacket = buildExtendedStatusPacket(clientState, changeTo);
		default -> statePacket = null;
		}
		return statePacket;
	}

	private Packet buildSimpleStatusPacket(ClientState clientState, boolean changeTo) {
		final var remoteServerId = getRemoteServerIdIfExistent();

		if (remoteServerId != STANDARD_SERVER_ID) {
			final var simpleStatusDTO = new SimpleStatusSyncBuilder<>();
			simpleStatusDTO.withClientState(clientState).withToAdd(changeTo);
			return completeStatusPacket(remoteServerId, simpleStatusDTO);
		}
		return null;
	}

	private Packet buildExtendedStatusPacket(ClientState clientState, boolean changeTo) {
		final var extendedStatusDTO = new ExtendedStatusSyncBuilder<>();
		final var contactSet = this.contactListAccess.readContacts(EligibleContactEntity.CLIENT);
		final var groupSet = this.contactListAccess.readContacts(EligibleContactEntity.GROUP);
		final var recipientId = serverConnectionNodes.getLocalServerConnectionData().getServerId();

		contactSet.addAll(groupSet);
		extendedStatusDTO.withContactIdSet(contactSet).withClientState(clientState).withToAdd(changeTo);

		return completeStatusPacket(recipientId, extendedStatusDTO);
	}

	private Packet completeStatusPacket(final int recipientId, final SimpleStatusSyncBuilder<?> contentBuilder) {
		final var clientData = this.clientDataManager.getCommunicatorData();
		final var localServerId = ClientStatePublisher.serverConnectionNodes.getLocalServerConnectionData()
				.getServerId();

		contentBuilder.withContactData(clientData).withOriginatingServerId(localServerId);
		return PacketCompiler.createRequest(getServerEntity(recipientId), contentBuilder.build());
	}

	private int getRemoteServerIdIfExistent() {
		int remoteServerId = STANDARD_SERVER_ID;
		final var localServerId = serverConnectionNodes.getLocalServerConnectionData().getServerId();
		final var remoteServerNode = serverConnectionNodes.getDistinctNodeData(of(localServerId));

		if (remoteServerNode != null) {
			remoteServerId = remoteServerNode.getServerId();
		}
		return remoteServerId;
	}
}
