package de.vsy.server.service.status_synchronization;

import de.vsy.server.client_handling.data_management.PacketRetainer;
import de.vsy.server.data.PacketCategorySubscriptionManager;
import de.vsy.server.data.access.ClientStatusRegistrationServiceDataProvider;
import de.vsy.server.server_packet.content.ExtendedStatusSyncDTO;
import de.vsy.server.server_packet.dispatching.ServerSynchronizationPacketDispatcher;
import de.vsy.server.server_packet.packet_creation.ResultingPacketContentHandler;
import de.vsy.server.server_packet.packet_creation.ServerStatusSyncPacketCreator;
import de.vsy.server.service.Service;
import de.vsy.server.service.ServiceBase;
import de.vsy.server.service.ServiceData;
import de.vsy.server.service.ServicePacketBufferManager;
import de.vsy.server.service.packet_logic.ClientStatusPacketProcessorFactory;
import de.vsy.server.service.packet_logic.processor.ServicePacketProcessor;
import de.vsy.shared_module.packet_management.PacketBuffer;
import de.vsy.shared_module.packet_management.PacketDispatcher;
import de.vsy.shared_module.packet_management.PacketTransmissionCache;
import de.vsy.shared_transmission.packet.Packet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static de.vsy.shared_transmission.packet.property.packet_category.PacketCategory.CHAT;
import static de.vsy.shared_transmission.packet.property.packet_category.PacketCategory.STATUS;

/**
 * Service processing inter-server synchronization Packet.
 */
public class ClientStatusSynchronizationService extends ServiceBase {

    private static final ServiceData SERVICE_SPECIFICATIONS;
    private static final Logger LOGGER = LogManager.getLogger();

    static {
        SERVICE_SPECIFICATIONS = ServiceData.ServiceDataBuilder.create()
                .withType(Service.TYPE.CHAT_STATUS_UPDATE)
                .withName("ClientSyncService")
                .withDirection(ServiceData.ServiceResponseDirection.INBOUND, Service.TYPE.REQUEST_ROUTER)
                .withDirection(ServiceData.ServiceResponseDirection.OUTBOUND, Service.TYPE.SERVER_TRANSFER)
                .build();
    }

    private final ServicePacketProcessor processor;
    private final PacketCategorySubscriptionManager serverBoundNetwork;
    private final PacketCategorySubscriptionManager clientBoundNetwork;
    private final ServicePacketBufferManager serviceBuffers;
    private final PacketTransmissionCache packetsToSend;
    private final ServerStatusSyncPacketCreator packetCreator;
    private final PacketDispatcher dispatcher;
    private PacketBuffer incomingBuffer;

    /**
     * Instantiates a new client status synchronizing service.
     *
     * @param serviceDataModel the dataManagement manager
     */
    public ClientStatusSynchronizationService(
            final ClientStatusRegistrationServiceDataProvider serviceDataModel) {
        super(SERVICE_SPECIFICATIONS, serviceDataModel.getLocalServerConnectionData());
        this.serviceBuffers = serviceDataModel.getServicePacketBufferManager();
        this.clientBoundNetwork = serviceDataModel.getClientSubscriptionManager();
        this.serverBoundNetwork = serviceDataModel.getServiceSubscriptionManager();
        this.packetCreator = new ServerStatusSyncPacketCreator();
        this.packetsToSend = new PacketTransmissionCache();
        var resultingPackets = new ResultingPacketContentHandler(packetCreator, packetsToSend);
        this.processor = new ServicePacketProcessor(
                new ClientStatusPacketProcessorFactory(resultingPackets, serviceDataModel),
                resultingPackets);
        this.dispatcher = new ServerSynchronizationPacketDispatcher(this.serviceBuffers,
                SERVICE_SPECIFICATIONS.getResponseDirections());
    }

    @Override
    public void finishSetup() {
        final var serviceId = super.getServiceId();

        this.incomingBuffer = this.serviceBuffers.registerBuffer(
                SERVICE_SPECIFICATIONS.getServiceType(), serviceId);
        this.serverBoundNetwork.subscribe(STATUS, serviceId, incomingBuffer);
        super.setReadyState();
    }

    @Override
    public void work() {
        Packet input;

        try {
            input = incomingBuffer.getPacket();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            LOGGER.error("Interrupted while waiting for next Packet.");
            input = null;
        }

        if (input != null) {
            this.packetCreator.setCurrentPacket(input);
            this.processor.processPacket(input);
            this.packetsToSend.transmitPackets(this.dispatcher);
        }
    }

    @Override
    public void breakDown() {
        final var serviceId = super.getServiceId();
        this.serverBoundNetwork.unsubscribe(STATUS, serviceId, incomingBuffer);
        this.serviceBuffers.deregisterBuffer(getServiceType(), serviceId, this.incomingBuffer);
        retainPackets();
    }

    private void retainPackets() {
        final var remainingPackets = this.incomingBuffer.freezeBuffer();

        for (final var packet : remainingPackets) {

            if (packet.getPacketContent() instanceof ExtendedStatusSyncDTO extendedStatusSyncDTO) {
                var clients = this.clientBoundNetwork.getThreads(CHAT);
                clients.removeIf((client) -> !(extendedStatusSyncDTO.getContactIdSet().contains(client)));
                PacketRetainer.retainExtendedStatus(extendedStatusSyncDTO, clients);
            } else {
                LOGGER.error("Packet discarded: {}", packet);
            }
        }
    }
}
