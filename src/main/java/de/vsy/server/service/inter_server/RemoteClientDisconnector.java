package de.vsy.server.service.inter_server;

import de.vsy.server.client_handling.strategy.VolatilePacketIdentifier;
import de.vsy.server.client_management.ClientState;
import de.vsy.server.client_management.ClientStateTranslator;
import de.vsy.server.data.PacketCategorySubscriptionManager;
import de.vsy.server.persistent_data.client_data.ContactListDAO;
import de.vsy.server.persistent_data.client_data.PendingPacketDAO;
import de.vsy.server.persistent_data.data_bean.ConvertCommDataToDTO;
import de.vsy.server.persistent_data.server_data.CommunicatorPersistenceDAO;
import de.vsy.server.persistent_data.server_data.temporal.LiveClientStateDAO;
import de.vsy.server.server_packet.content.builder.ExtendedStatusSyncBuilder;
import de.vsy.server.service.Service;
import de.vsy.server.service.ServicePacketBufferManager;
import de.vsy.shared_module.packet_creation.PacketCompiler;
import de.vsy.shared_module.packet_management.PacketBuffer;
import de.vsy.shared_transmission.packet.content.relation.EligibleContactEntity;

import java.util.Map;
import java.util.Set;

import static de.vsy.server.persistent_data.client_data.PendingType.PROCESSOR_BOUND;
import static de.vsy.shared_transmission.packet.property.communicator.CommunicationEndpoint.getServerEntity;
import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_SERVER_ID;

public class RemoteClientDisconnector {

    private final PacketBuffer remoteServerBuffer;
    private final ServicePacketBufferManager serviceBufferManager;
    private final CommunicatorPersistenceDAO communicatorDataProvider;
    private final LiveClientStateDAO clientStateProvider;
    private final PacketCategorySubscriptionManager clientSubscriptionManager;

    public RemoteClientDisconnector(final PacketBuffer remoteServerBuffer,
                                    final ServicePacketBufferManager serviceBufferManager,
                                    final CommunicatorPersistenceDAO communicatorDataProvider,
                                    final LiveClientStateDAO clientStateProvider,
                                    final PacketCategorySubscriptionManager clientSubscriptionManager) {
        this.remoteServerBuffer = remoteServerBuffer;
        this.serviceBufferManager = serviceBufferManager;
        this.communicatorDataProvider = communicatorDataProvider;
        this.clientStateProvider = clientStateProvider;
        this.clientSubscriptionManager = clientSubscriptionManager;
    }

    public void disconnectRemainingClients(
            final Map<Integer, PendingPacketDAO> clientPersistenceAccessManagers) {

        for (final var currentClient : clientPersistenceAccessManagers.entrySet()) {
            final var clientId = currentClient.getKey();
            final var currentClientState = this.clientStateProvider.getClientState(clientId);
            final var currentPendingAccess = currentClient.getValue();
            disconnectClient(clientId, currentClientState.getCurrentState());
            removeVolatilePackets(currentPendingAccess);
            currentPendingAccess.removeFileAccess();
        }
    }

    private void disconnectClient(int clientId, ClientState currentState) {
        publishState(clientId, currentState);
        unsubscribeClient(clientId);
        this.clientStateProvider.removeClientState(clientId);
    }

    private void removeVolatilePackets(PendingPacketDAO pendingPacketAccess) {
        var pendingPacketMap = pendingPacketAccess.readPendingPackets(PROCESSOR_BOUND);
        pendingPacketMap.values().removeIf(VolatilePacketIdentifier::checkPacketVolatility);
        pendingPacketAccess.setPendingPackets(PROCESSOR_BOUND, pendingPacketMap);
    }

    private void publishState(final int clientId, final ClientState currentState) {
        Set<Integer> contactIdList;
        final var contactListProvider = new ContactListDAO();

        contactListProvider.createFileAccess(clientId);
        contactIdList = contactListProvider.readContacts(EligibleContactEntity.CLIENT);

        if (!contactIdList.isEmpty()) {
            final var statusSyncBuilder = new ExtendedStatusSyncBuilder<>();
            final var requestAssignmentBuffer = this.serviceBufferManager.getRandomBuffer(
                    Service.TYPE.REQUEST_ROUTER);

            if (requestAssignmentBuffer != null) {
                final var communicatorData = this.communicatorDataProvider.getCommunicatorData(clientId);

                statusSyncBuilder.withContactIdSet(contactIdList)
                        .withContactData(ConvertCommDataToDTO.convertFrom(communicatorData))
                        .withClientState(currentState).withToAdd(false);

                requestAssignmentBuffer.appendPacket(
                        PacketCompiler.createRequest(getServerEntity(STANDARD_SERVER_ID),
                                statusSyncBuilder.build()));
            }
        }
    }

    public void unsubscribeClient(final int clientId) {
        final var subscriptionMap = ClientStateTranslator.prepareClientSubscriptionMap(
                ClientState.AUTHENTICATED, false,
                clientId);
        final var extraSubscriptionMap = this.clientStateProvider.getAllExtraSubscriptions(clientId);

        for (final var subscriptionSet : subscriptionMap.entrySet()) {
            final var topic = subscriptionSet.getKey();
            final var threads = subscriptionSet.getValue();

            if (extraSubscriptionMap.containsKey(topic)) {
                threads.addAll(extraSubscriptionMap.get(topic));
            }
            for (final var currentThread : threads) {
                this.clientSubscriptionManager.unsubscribe(topic, currentThread, this.remoteServerBuffer);
            }
        }
    }
}
