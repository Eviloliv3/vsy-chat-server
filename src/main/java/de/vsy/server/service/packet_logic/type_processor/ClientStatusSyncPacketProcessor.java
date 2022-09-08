/*
 *
 */
package de.vsy.server.service.packet_logic.type_processor;

import de.vsy.server.client_handling.data_management.logic.SubscriptionHandler;
import de.vsy.server.persistent_data.server_data.temporal.LiveClientStateDAO;
import de.vsy.server.server.client_management.ClientStateTranslator;
import de.vsy.server.server.data.AbstractPacketCategorySubscriptionManager;
import de.vsy.server.server.data.access.ClientStatusRegistrationServiceDataProvider;
import de.vsy.server.server.server_connection.LocalServerConnectionData;
import de.vsy.server.server.server_connection.RemoteServerConnectionData;
import de.vsy.server.server.server_connection.ServerConnectionDataManager;
import de.vsy.server.server_packet.content.BaseStatusSyncDTO;
import de.vsy.server.server_packet.content.ExtendedStatusSyncDTO;
import de.vsy.server.server_packet.content.ServerPacketContentImpl;
import de.vsy.server.server_packet.packet_creation.ResultingPacketContentHandler;
import de.vsy.server.service.Service;
import de.vsy.server.service.ServicePacketBufferManager;
import de.vsy.server.service.packet_logic.PacketResponseMap;
import de.vsy.server.service.packet_logic.ServicePacketProcessor;
import de.vsy.shared_module.shared_module.packet_exception.PacketProcessingException;
import de.vsy.shared_transmission.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.shared_transmission.packet.property.packet_category.PacketCategory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Set;

import static de.vsy.shared_transmission.shared_transmission.packet.property.communicator.CommunicationEndpoint.getClientEntity;
import static de.vsy.shared_transmission.shared_transmission.packet.property.communicator.CommunicationEndpoint.getServerEntity;
import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_CLIENT_BROADCAST_ID;

/** Handles status synchronization Packet sent by other servers. */
public
class ClientStatusSyncPacketProcessor implements ServicePacketProcessor {

    private static final Logger LOGGER = LogManager.getLogger();
    private final ServerConnectionDataManager serverConnectionDataManager;
    private final LocalServerConnectionData serverNode;
    private final ServicePacketBufferManager serviceBufferManager;
    private final AbstractPacketCategorySubscriptionManager clientSubscriptionManager;
    private ResultingPacketContentHandler resultingPackets;
    private final LiveClientStateDAO persistentClientStates;

    /**
     * Instantiates a new client status sync PacketHandler.
     *
     * @param serviceDataAccess the service dataManagement accessLimiter
     */
    public
    ClientStatusSyncPacketProcessor (
            final ClientStatusRegistrationServiceDataProvider serviceDataAccess) {
        this.serverConnectionDataManager = serviceDataAccess.getServerConnectionDataManager();
        this.serverNode = this.serverConnectionDataManager.getLocalServerConnectionData();
        this.serviceBufferManager = serviceDataAccess.getServicePacketBufferManager();
        this.clientSubscriptionManager = serviceDataAccess.getClientSubscriptionManager();
        this.persistentClientStates = serviceDataAccess.getLiveClientStateDAO();
    }

    /**
     * Zuerst werden die Subscriptions angepasst, sofern die Nachricht von einem
     * entfernten Server kommt. Der Server wird als synchronisiert auf den Paketdaten
     * eingetragen. Zuletzt werden, sofern erforderlich, Klienten informiert.
     *
     * @param input das zu verarbeitende Paket
     *
     * @throws PacketProcessingException the PacketHandling exception
     */
    @Override
    public
    void processPacket (final Packet input)
    throws PacketProcessingException {
        RemoteServerConnectionData notSynchronizedServerData;
        final var content = input.getPacketContent();

        if (content instanceof final ServerPacketContentImpl inputData) {
            final var originatingServerId = inputData.getOriginatingServerId();

            if (originatingServerId != this.serverNode.getServerId() &&
                inputData instanceof final BaseStatusSyncDTO simpleStatus) {
                LogManager.getLogger().debug("BaseStatusSyncDTO gelesen: {}", simpleStatus);
                translateState(simpleStatus, originatingServerId);
            }

            if (inputData instanceof ExtendedStatusSyncDTO) {
                LogManager.getLogger().debug("ExtendedStatusSyncDTO gelesen: {}", inputData);
                final var clientBroadcast = getClientEntity(STANDARD_CLIENT_BROADCAST_ID);
                resultingPackets.addRequest(inputData, clientBroadcast);
            }

            notSynchronizedServerData = this.serverConnectionDataManager.getDistinctNodeData(
                    inputData.getSynchronizedServers());

            if (notSynchronizedServerData != null) {
                LogManager.getLogger().debug("Statussynchronisierung fuer anderen Server erstellt.");
                final var recipient = getServerEntity(
                        notSynchronizedServerData.getServerId());
                resultingPackets.addRequest(inputData, recipient);
            }
        } else {
            throw new PacketProcessingException(
                    "Inhalt nicht vom Typ " + "ServerPacketContentImpl. Konnte " +
                    "nicht von ClientStatusProcessor " + "verarbeitet werden.");
        }
    }

    /**
     * Translate state.
     *
     * @param clientStatusData the messenger status
     */
    private
    void translateState (final BaseStatusSyncDTO clientStatusData,
                         final int serviceId) {
        final var remoteClientBuffer = this.serviceBufferManager.getSpecificBuffer(
                Service.TYPE.SERVER_TRANSFER, serviceId);
        final var subscriptions = createSubscriptionMap(clientStatusData);
        SubscriptionHandler subscriptionLogic = clientStatusData.isToAdd() ? this.clientSubscriptionManager::subscribe : this.clientSubscriptionManager::unsubscribe;

        for (final var currentSubscriptionSet : subscriptions.entrySet()) {
            final var currentTopic = currentSubscriptionSet.getKey();
            final var currentThreadSet = currentSubscriptionSet.getValue();

            for (final var currentThread : currentThreadSet) {
                subscriptionLogic.handle(currentTopic, currentThread,
                                         remoteClientBuffer);
            }
        }
    }

    private
    Map<PacketCategory, Set<Integer>> createSubscriptionMap(final BaseStatusSyncDTO statusSync){
        final var clientState = statusSync.getClientState();
        final var clientId = statusSync.getContactData().getCommunicatorId();
        final var subscriptions = ClientStateTranslator.prepareClientSubscriptionMap(
                clientState, statusSync.isToAdd(),
                clientId);
        final var persistedClientState= this.persistentClientStates.getClientState(clientId);

        subscriptions.putAll(persistedClientState.getExtraSubscriptions());
        return subscriptions;
    }
}
