/*
 *
 */
package de.vsy.server.service.packet_logic.type_processor;

import de.vsy.server.client_handling.data_management.logic.SubscriptionHandler;
import de.vsy.server.server.client_management.ClientStateTranslator;
import de.vsy.server.server.data.AbstractPacketCategorySubscriptionManager;
import de.vsy.shared_module.shared_module.packet_creation.PacketCompiler;
import de.vsy.shared_module.shared_module.packet_exception.PacketProcessingException;
import de.vsy.server.server.data.access.ClientStatusRegistrationServiceDataProvider;
import de.vsy.server.server.server_connection.LocalServerConnectionData;
import de.vsy.server.server.server_connection.RemoteServerConnectionData;
import de.vsy.server.server.server_connection.ServerConnectionDataManager;
import de.vsy.server.server_packet.content.ExtendedStatusSyncDTO;
import de.vsy.server.server_packet.content.ServerPacketContentImpl;
import de.vsy.server.server_packet.content.SimpleStatusSyncDTO;
import de.vsy.server.service.Service;
import de.vsy.server.service.ServicePacketBufferManager;
import de.vsy.server.service.packet_logic.PacketResponseMap;
import de.vsy.server.service.packet_logic.ServicePacketProcessor;
import de.vsy.shared_transmission.shared_transmission.packet.Packet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static de.vsy.shared_transmission.shared_transmission.packet.property.communicator.CommunicationEndpoint.getServerEntity;

/** Handles status synchronization Packet sent by other servers. */
public
class ClientStatusSyncPacketProcessor implements ServicePacketProcessor {

    private static final Logger LOGGER = LogManager.getLogger();
    private final ServerConnectionDataManager serverConnectionDataManager;
    private final LocalServerConnectionData serverNode;
    private final ServicePacketBufferManager serviceBufferManager;
    private final AbstractPacketCategorySubscriptionManager clientSubscriptionManager;

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
    }

    /**
     * Zuerst werden die Subscriptions angepasst, sofern die Nachricht von einem
     * entfernten Server kommt. Der Server wird als synchronisiert auf den Paketdaten
     * eingetragen. Zuletzt werden, sofern erforderlich, Klienten informiert.
     *
     * @param input das zu verarbeitende Paket
     *
     * @return Server gerichtetes Paket, sofern ein Server noch nicht synchronisiert
     *         wurde.
     *
     * @throws PacketProcessingException the PacketHandling exception
     */
    @Override
    public
    PacketResponseMap processPacket (final Packet input)
    throws PacketProcessingException {
        RemoteServerConnectionData notSynchronizedServerData;
        final var responses = new PacketResponseMap();
        final var content = input.getPacketContent();

        if (content instanceof final ServerPacketContentImpl inputData) {
            final var originatingServerId = inputData.getOriginatingServerId();

            if (originatingServerId != this.serverNode.getServerId() &&
                inputData instanceof final SimpleStatusSyncDTO simpleStatus) {
                translateState(simpleStatus, originatingServerId);
            }
            inputData.addSyncedServerId(this.serverNode.getServerId());
            notSynchronizedServerData = this.serverConnectionDataManager.getDistinctNodeData(
                    inputData.getSyncedServers());

            if (notSynchronizedServerData != null) {
                final var recipient = getServerEntity(
                        notSynchronizedServerData.getServerId());
                final var serverNotification = PacketCompiler.createRequest(
                        recipient, inputData);
                responses.setServerBoundPacket(serverNotification);
            }

            if (inputData instanceof ExtendedStatusSyncDTO) {
                final var clientBroadcast = getServerEntity(
                        this.serverConnectionDataManager.getLocalServerConnectionData()
                                                        .getServerId());
                final var clientNotification = PacketCompiler.createRequest(
                        clientBroadcast, inputData);
                responses.setClientBoundPacket(clientNotification);
            }
        } else {
            throw new PacketProcessingException(
                    "Inhalt nicht vom Typ " + "ServerPacketContentImpl. Konnte " +
                    "nicht von ClientStatusProcessor " + "verarbeitet werden.");
        }
        return responses;
    }

    /**
     * Translate state.
     *
     * @param messengerStatus the messenger status
     */
    private
    void translateState (final SimpleStatusSyncDTO messengerStatus,
                         final int serviceId) {
        final var clientState = messengerStatus.getClientState();
        final var clientData = messengerStatus.getContactData();
        final var remoteClientBuffer = this.serviceBufferManager.getSpecificBuffer(
                Service.TYPE.SERVER_TRANSFER, serviceId);
        final var subscriptions = ClientStateTranslator.prepareClientSubscriptionMap(
                clientState, messengerStatus.isToAdd(),
                clientData.getCommunicatorId());
        SubscriptionHandler subscriptionLogic = messengerStatus.isToAdd() ? this.clientSubscriptionManager::subscribe : this.clientSubscriptionManager::unsubscribe;

        for (final var currentSubscriptionSet : subscriptions.entrySet()) {
            final var currentTopic = currentSubscriptionSet.getKey();
            final var currentThreadSet = currentSubscriptionSet.getValue();

            for (final var currentThread : currentThreadSet) {
                subscriptionLogic.handle(currentTopic, currentThread,
                                         remoteClientBuffer);
            }
        }
    }
}
