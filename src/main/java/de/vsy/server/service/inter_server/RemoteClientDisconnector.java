package de.vsy.server.service.inter_server;

import de.vsy.server.persistent_data.client_data.ContactListDAO;
import de.vsy.server.persistent_data.client_data.PendingPacketDAO;
import de.vsy.server.persistent_data.data_bean.ConvertCommDataToDTO;
import de.vsy.server.persistent_data.server_data.CommunicatorPersistenceDAO;
import de.vsy.server.persistent_data.server_data.temporal.LiveClientStateDAO;
import de.vsy.server.server.client_management.ClientState;
import de.vsy.server.server.client_management.ClientStateTranslator;
import de.vsy.server.server.data.AbstractPacketCategorySubscriptionManager;
import de.vsy.server.server_packet.content.builder.ExtendedStatusSyncBuilder;
import de.vsy.server.service.Service;
import de.vsy.server.service.ServicePacketBufferManager;
import de.vsy.shared_module.shared_module.packet_creation.PacketCompiler;
import de.vsy.shared_module.shared_module.packet_management.PacketBuffer;
import de.vsy.shared_transmission.shared_transmission.packet.content.relation.EligibleContactEntity;
import org.apache.logging.log4j.LogManager;

import java.util.Map;
import java.util.Set;

import static de.vsy.shared_transmission.shared_transmission.packet.property.communicator.CommunicationEndpoint.getServerEntity;
import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_SERVER_ID;

public
class RemoteClientDisconnector {

    private final PacketBuffer remoteServerBuffer;
    private final ServicePacketBufferManager serviceBufferManager;
    private final CommunicatorPersistenceDAO communicatorDataProvider;
    private final LiveClientStateDAO clientStateProvider;
    private final AbstractPacketCategorySubscriptionManager clientSubscriptionManager;

    public
    RemoteClientDisconnector (final PacketBuffer remoteServerBuffer,
                              final ServicePacketBufferManager serviceBufferManager,
                              final CommunicatorPersistenceDAO communicatorDataProvider,
                              final LiveClientStateDAO clientStateProvider,
                              final AbstractPacketCategorySubscriptionManager clientSubscriptionManager) {
        this.remoteServerBuffer = remoteServerBuffer;
        this.serviceBufferManager = serviceBufferManager;
        this.communicatorDataProvider = communicatorDataProvider;
        this.clientStateProvider = clientStateProvider;
        this.clientSubscriptionManager = clientSubscriptionManager;
    }

    public
    void disconnectRemainingClients (
            final Map<Integer, PendingPacketDAO> clientPersistenceAccessManagers) {

        for (final var currentClient : clientPersistenceAccessManagers.entrySet()) {
            final var clientId = currentClient.getKey();
            final var currentState = this.clientStateProvider.getClientState(
                    clientId);
            disconnectClient(clientId, currentState.getCurrentState());
            currentClient.getValue().removeFileAccess();
        }
    }

    private
    void disconnectClient (int clientId, ClientState currentState) {
        try {
            publishState(clientId, currentState);
        } catch (InterruptedException ie) {
            LogManager.getLogger()
                      .error("Kein Kontaktlistenzugriff fuer " +
                             "Klienten {}. Klientenstatus wurde Kontakten " +
                             "nicht mitgeteilt.", clientId);
        }
        unsubscribeClient(clientId);
        this.clientStateProvider.removeClientState(clientId);
    }

    private
    void publishState (final int clientId, final ClientState currentState)
    throws InterruptedException {
        Set<Integer> contactIdList;
        final var contactListProvider = new ContactListDAO();

        contactListProvider.createFileAccess(clientId);
        contactIdList = contactListProvider.readContacts(
                EligibleContactEntity.CLIENT);

        if (!contactIdList.isEmpty()) {
            final var statusSyncBuilder = new ExtendedStatusSyncBuilder<>();
            final var requestAssignmentBuffer = this.serviceBufferManager.getRandomBuffer(
                    Service.TYPE.REQUEST_ROUTER);

            if (requestAssignmentBuffer != null) {
                final var communicatorData = this.communicatorDataProvider.getCommunicatorData(
                        clientId);

                statusSyncBuilder.withContactSet(contactIdList)
                                 .withCommunicatorData(
                                         ConvertCommDataToDTO.convertFrom(
                                                 communicatorData))
                                 .withClientState(currentState)
                                 .withIsToAdd(false);

                requestAssignmentBuffer.appendPacket(PacketCompiler.createRequest(
                        getServerEntity(STANDARD_SERVER_ID),
                        statusSyncBuilder.build()));
            }
        }
    }

    public
    void unsubscribeClient (final int clientId) {
        final var subscriptionMap = ClientStateTranslator.prepareClientSubscriptionMap(
                ClientState.AUTHENTICATED, false, clientId);
        final var extraSubscriptionMap = this.clientStateProvider.getAllExtraSubscriptions(
                clientId);

        for (final var subscriptionSet : subscriptionMap.entrySet()) {
            final var topic = subscriptionSet.getKey();
            final var threads = subscriptionSet.getValue();

            if (extraSubscriptionMap.containsKey(topic)) {
                threads.addAll(extraSubscriptionMap.get(topic));
            }
            for (final var currentThread : threads) {
                this.clientSubscriptionManager.unsubscribe(topic, currentThread,
                                                           this.remoteServerBuffer);
            }
        }
    }
}
