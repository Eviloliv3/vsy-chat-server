/*
 *
 */
package de.vsy.server.service.status_synchronization;

import de.vsy.server.server.data.AbstractPacketCategorySubscriptionManager;
import de.vsy.server.server.data.access.ClientStatusRegistrationServiceDataProvider;
import de.vsy.server.service.Service;
import de.vsy.server.service.ServiceBase;
import de.vsy.server.service.ServiceData;
import de.vsy.server.service.ServicePacketBufferManager;
import de.vsy.server.service.packet_logic.ClientStatusPacketProcessorFactory;
import de.vsy.server.service.packet_logic.processor.ServicePacketProcessor;
import de.vsy.shared_module.shared_module.packet_management.PacketBuffer;
import de.vsy.shared_transmission.shared_transmission.packet.Packet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static de.vsy.shared_transmission.shared_transmission.packet.property.packet_category.PacketCategory.STATUS;

/** Service processing inter-server synchronization Packet. */
public
class ClientStatusSynchronizingService extends ServiceBase {

    private static final ServiceData SERVICE_SPECIFICATIONS;
    private static final Logger LOGGER = LogManager.getLogger();
    private final ServicePacketProcessor processor;
    private final AbstractPacketCategorySubscriptionManager serverBoundNetwork;
    private final ServicePacketBufferManager serviceBuffers;
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
    ClientStatusSynchronizingService (
            final ClientStatusRegistrationServiceDataProvider serviceDataModel) {
        super(SERVICE_SPECIFICATIONS,
              serviceDataModel.getServicePacketBufferManager(),
              serviceDataModel.getLocalServerConnectionData());
        this.serviceBuffers = serviceDataModel.getServicePacketBufferManager();
        this.serverBoundNetwork = serviceDataModel.getServiceSubscriptionManager();
        this.processor = new ServicePacketProcessor(
                new ClientStatusPacketProcessorFactory(serviceDataModel));
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
            LOGGER.info("Synchronisierung gestartet.");
            final var responseMap = this.processor.processPacket(input);

            super.dispatchResponsePacketMap(responseMap);
        }
    }

    /** Break down. */
    @Override
    public
    void breakDown () {
        this.serviceBuffers.deregisterBuffer(getServiceType(), getServiceId(),
                                             this.incomingBuffer);
    }
}