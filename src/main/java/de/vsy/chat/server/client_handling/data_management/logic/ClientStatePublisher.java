package de.vsy.chat.server.client_handling.data_management.logic;

import de.vsy.chat.shared_module.packet_creation.PacketCompiler;
import de.vsy.chat.server.client_handling.data_management.bean.ClientStateListener;
import de.vsy.chat.server.client_handling.data_management.bean.LocalClientDataProvider;
import de.vsy.chat.server.persistent_data.client_data.ContactListDAO;
import de.vsy.chat.server.server.client_management.ClientState;
import de.vsy.chat.server.server.server_connection.ServerConnectionDataManager;
import de.vsy.chat.server.server_packet.content.builder.ExtendedStatusSyncBuilder;
import de.vsy.chat.server.server_packet.content.builder.SimpleStatusSyncBuilder;
import de.vsy.chat.server.server_packet.dispatching.PacketDispatcher;
import de.vsy.chat.shared_transmission.packet.Packet;
import de.vsy.chat.shared_transmission.packet.content.relation.EligibleContactEntity;
import org.apache.logging.log4j.LogManager;

import static de.vsy.chat.shared_transmission.packet.property.communicator.CommunicationEndpoint.getServerEntity;
import static de.vsy.chat.shared_utility.standard_value.StandardIdProvider.STANDARD_SERVER_ID;
import static java.util.Set.of;

public
class ClientStatePublisher implements ClientStateListener {

    private static ServerConnectionDataManager serverConnectionNodes;
    private final LocalClientDataProvider clientDataManager;
    private final ContactListDAO contactListAccess;
    private final PacketDispatcher dispatcher;

    public
    ClientStatePublisher (final LocalClientDataProvider clientDataManager,
                          final ContactListDAO contactListProvider,
                          final PacketDispatcher dispatcher) {
        this.clientDataManager = clientDataManager;
        this.contactListAccess = contactListProvider;
        this.dispatcher = dispatcher;
    }

    public static
    void setupStaticServerDataAccess (
            final ServerConnectionDataManager serverConnectionDataManager) {
        serverConnectionNodes = serverConnectionDataManager;
    }

    @Override
    public
    void evaluateNewState (ClientState clientState, boolean changeTo) {
        final var statusPacket = createStatusSyncPacket(clientState, changeTo);

        if (statusPacket != null) {
            dispatcher.dispatchPacket(statusPacket);
        }
    }

    public
    Packet createStatusSyncPacket (ClientState clientState, boolean changeTo) {
        Packet statePacket;

        switch (clientState) {
            case AUTHENTICATED ->
                    statePacket = buildSimpleStatusPacket(clientState, changeTo);
            case ACTIVE_MESSENGER -> {
                statePacket = buildExtendedStatusPacket(clientState, changeTo);
                LogManager.getLogger()
                          .info("ExtendedStatusSync erzeugt:\n{}", statePacket);
            }
            default -> statePacket = null;
        }
        return statePacket;
    }

    private
    Packet buildSimpleStatusPacket (ClientState clientState, boolean changeTo) {
        Packet statusPacket = null;
        final var simpleStatusDTO = new SimpleStatusSyncBuilder<>();
        final var clientData = this.clientDataManager.getCommunicatorData();
        final var remoteServerId = getRemoteServerIdIfExistent();

        if (remoteServerId != STANDARD_SERVER_ID) {
            simpleStatusDTO.withClientState(clientState)
                           .withCommunicatorData(clientData)
                           .withIsToAdd(changeTo);
            statusPacket = PacketCompiler.createRequest(
                    getServerEntity(remoteServerId), simpleStatusDTO.build());
        }
        return statusPacket;
    }

    private
    Packet buildExtendedStatusPacket (ClientState clientState, boolean changeTo) {
        int recipientId;
        final var extendedStatusDTO = new ExtendedStatusSyncBuilder<>();
        final var clientData = this.clientDataManager.getCommunicatorData();
        final var contactSet = this.contactListAccess.readContacts(
                EligibleContactEntity.CLIENT);
        final var groupSet = this.contactListAccess.readContacts(
                EligibleContactEntity.GROUP);
        final var remoteServerId = getRemoteServerIdIfExistent();

        contactSet.addAll(groupSet);

        if (remoteServerId != STANDARD_SERVER_ID) {
            recipientId = remoteServerId;
        } else {
            recipientId = serverConnectionNodes.getLocalServerConnectionData()
                                               .getServerId();
        }

        extendedStatusDTO.withContactSet(contactSet)
                         .withClientState(clientState)
                         .withIsToAdd(changeTo)
                         .withCommunicatorData(clientData);
        return PacketCompiler.createRequest(getServerEntity(recipientId),
                                            extendedStatusDTO.build());
    }

    private
    int getRemoteServerIdIfExistent () {
        int remoteServerId = STANDARD_SERVER_ID;
        final var localServerId = serverConnectionNodes.getLocalServerConnectionData()
                                                       .getServerId();
        final var remoteServerNode = serverConnectionNodes.getDistinctNodeData(
                of(localServerId));

        if (remoteServerNode != null) {
            remoteServerId = remoteServerNode.getServerId();
        }
        return remoteServerId;
    }
}
