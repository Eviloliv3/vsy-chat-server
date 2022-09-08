/*
 *
 */
package de.vsy.server.service.status_synchronization;

import com.fasterxml.jackson.databind.deser.std.StringArrayDeserializer;
import de.vsy.server.server.data.AbstractPacketCategorySubscriptionManager;
import de.vsy.server.server.data.access.ClientStatusRegistrationServiceDataProvider;
import de.vsy.server.server_packet.dispatching.PacketDispatcher;
import de.vsy.server.server_packet.dispatching.PacketTransmissionCache;
import de.vsy.server.server_packet.dispatching.ServerSynchronizationPacketDispatcher;
import de.vsy.server.server_packet.packet_creation.ResultingPacketContentHandler;
import de.vsy.server.server_packet.packet_creation.ServerStatusSyncPacketCreator;
import de.vsy.server.service.Service;
import de.vsy.server.service.ServiceBase;
import de.vsy.server.service.ServiceData;
import de.vsy.server.service.ServicePacketBufferManager;
import de.vsy.server.service.packet_logic.ClientStatusPacketProcessorFactory;
import de.vsy.server.service.packet_logic.PacketResponseMap;
import de.vsy.server.service.packet_logic.processor.ServicePacketProcessor;
import de.vsy.shared_module.shared_module.packet_exception.PacketHandlingException;
import de.vsy.shared_module.shared_module.packet_management.PacketBuffer;
import de.vsy.shared_transmission.shared_transmission.packet.Packet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static de.vsy.shared_transmission.shared_transmission.packet.property.packet_category.PacketCategory.STATUS;

/** Service processing inter-server synchronization Packet. */
public
class ClientStatusSynchronizationService extends ServiceBase {

    private static final ServiceData SERVICE_SPECIFICATIONS;
    private static final Logger LOGGER = LogManager.getLogger();
    private final ServicePacketProcessor processor;
    private final AbstractPacketCategorySubscriptionManager serverBoundNetwork;
    private final ServicePacketBufferManager serviceBuffers;
    private final PacketTransmissionCache packetsToSend;
    private final ServerStatusSyncPacketCreator packetCreator;
    private final PacketDispatcher dispatcher;
    private PacketBuffer incomingBuffer;

    static {
        SERVICE_SPECIFICATIONS = ServiceData.ServiceDataBuilder.create()
                                                               .withType(
                                                                       Service.TYPE.CHAT_STATUS_UPDATE)
                                                               .withName(
                                                                       "ClientSyncService")
                                                               .withDirection(
                                                                       ServiceData.ServiceResponseDirection.INBOUND,
                                                                       Service.TYPE.REQUEST_ROUTER)
                                                               .withDirection(
                                                                       ServiceData.ServiceResponseDirection.OUTBOUND,
                                                                       Service.TYPE.SERVER_TRANSFER)
                                                               .build();
    }

    /**
     * Instantiates a new client status synchronizing service.
     *
     * @param serviceDataModel the dataManagement manager
     */
    public
    ClientStatusSynchronizationService (
            final ClientStatusRegistrationServiceDataProvider serviceDataModel) {
        super(SERVICE_SPECIFICATIONS,
              serviceDataModel.getServicePacketBufferManager(),
              serviceDataModel.getLocalServerConnectionData());
        this.serviceBuffers = serviceDataModel.getServicePacketBufferManager();
        this.serverBoundNetwork = serviceDataModel.getServiceSubscriptionManager();
        this.packetCreator = new ServerStatusSyncPacketCreator();
        this.packetsToSend = new PacketTransmissionCache();
        this.processor = new ServicePacketProcessor(
                new ClientStatusPacketProcessorFactory(serviceDataModel),
                new ResultingPacketContentHandler(packetCreator, packetsToSend));
        this.dispatcher = new ServerSynchronizationPacketDispatcher(this.serviceBuffers,
                                                                    SERVICE_SPECIFICATIONS.getResponseDirections());
    }

    /** Finish setup. */
    @Override
    public
    void finishSetup () {
        final var serviceId = super.getServiceId();

        this.incomingBuffer = this.serviceBuffers.registerBuffer(
                SERVICE_SPECIFICATIONS.getServiceType(), serviceId);
        this.serverBoundNetwork.subscribe(STATUS, serviceId, incomingBuffer);
        super.setReadyState();
    }

    /**
     * Checks if is interruption condition met.
     *
     * @return true, if is interruption condition met
     */
    @Override
    public
    boolean interruptionConditionNotMet () {
        return !Thread.currentThread().isInterrupted();
    }

    /** Work. */
    @Override
    public
    void work () {
        Packet input;

        try {
            input = incomingBuffer.getPacket();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            LOGGER.error("Beim Holen des naechsten Pakets unterbrochen.");
            input = null;
        }

        if (input != null) {
            this.packetCreator.changeCurrentRequest(input);
            this.processor.processPacket(input);
            this.packetsToSend.transmitPackets(this.dispatcher);
        }
    }

    /** Break down. */
    @Override
    public
    void breakDown () {
        this.serviceBuffers.deregisterBuffer(getServiceType(), getServiceId(),
                                             this.incomingBuffer);
    }

    /**
     * Dispatch response Packetmap.
     *
     * @param responseMap the response map
     */
    private
    void dispatchResponsePacketMap (final PacketResponseMap responseMap) {
        Packet toDispatch;

        if (responseMap != null) {
            toDispatch = responseMap.getClientBoundPacket();

            if (toDispatch != null) {
                this.dispatcher.dispatchPacket(toDispatch);
            }
            toDispatch = responseMap.getServerBoundPacket();

            if (toDispatch != null) {
                this.dispatcher.dispatchPacket(toDispatch);
            }
        }
    }
}
