package de.vsy.server.service.request;

import de.vsy.server.client_handling.data_management.PacketRetainer;
import de.vsy.server.data.PacketCategorySubscriptionManager;
import de.vsy.server.data.access.PacketAssignmentServiceDataProvider;
import de.vsy.server.exception_processing.ServerPacketHandlingExceptionCreator;
import de.vsy.server.server_packet.content.ExtendedStatusSyncDTO;
import de.vsy.server.server_packet.packet_validation.ServerPermittedCategoryContentAssociationProvider;
import de.vsy.server.service.*;
import de.vsy.server.service.ServiceData.ServiceDataBuilder;
import de.vsy.server.service.ServiceData.ServiceResponseDirection;
import de.vsy.shared_module.exception_processing.PacketHandlingExceptionProcessor;
import de.vsy.shared_module.packet_exception.PacketHandlingException;
import de.vsy.shared_module.packet_exception.PacketProcessingException;
import de.vsy.shared_module.packet_exception.PacketTransmissionException;
import de.vsy.shared_module.packet_management.PacketBuffer;
import de.vsy.shared_module.packet_validation.PacketCheck;
import de.vsy.shared_module.packet_validation.SimplePacketChecker;
import de.vsy.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.packet.property.communicator.EligibleCommunicationEntity;

import java.util.EnumMap;
import java.util.Map;

import static de.vsy.shared_transmission.packet.property.communicator.EligibleCommunicationEntity.CLIENT;
import static de.vsy.shared_transmission.packet.property.communicator.EligibleCommunicationEntity.SERVER;
import static de.vsy.shared_transmission.packet.property.packet_category.PacketCategory.CHAT;

/**
 * Service assigning clients' requests to the appropriate service.
 */
public class PacketAssignmentService extends ServiceBase {

    private static final ServiceData SERVICE_SPECIFICATIONS;

    static {
        SERVICE_SPECIFICATIONS = ServiceDataBuilder.create().withType(Service.TYPE.REQUEST_ROUTER)
                .withName("PacketAssignmentService")
                .withDirection(ServiceResponseDirection.INBOUND, Service.TYPE.REQUEST_ROUTER)
                .withDirection(ServiceResponseDirection.OUTBOUND, Service.TYPE.SERVER_TRANSFER).build();
    }

    private final ServicePacketBufferManager serviceBuffers;
    private PacketHandlingExceptionProcessor pheProcessor;
    private PublishablePacketCreator preProcessor;
    private CommunicationNetworkSubscriptionManager packetNetworkManager;
    private PacketBuffer requestBuffer;
    private PacketCheck validator;

    /**
     * Instantiates a new client request assignment service.
     *
     * @param serviceDataModel the service dataManagement model
     */
    public PacketAssignmentService(final PacketAssignmentServiceDataProvider serviceDataModel) {
        super(SERVICE_SPECIFICATIONS, serviceDataModel.getLocalServerConnectionData());
        this.serviceBuffers = serviceDataModel.getServicePacketBufferManager();
        setupPacketNetworkManager(serviceDataModel.getServiceSubscriptionManager(),
                serviceDataModel.getClientSubscriptionManager());
    }

    /**
     * Setup PacketNetwork manager.
     *
     * @param serverBoundNetwork the server bound network
     * @param clientBoundNetwork the client bound network
     */
    private void setupPacketNetworkManager(
            final PacketCategorySubscriptionManager serverBoundNetwork,
            final PacketCategorySubscriptionManager clientBoundNetwork) {
        Map<EligibleCommunicationEntity, PacketCategorySubscriptionManager> packetNetworkMap;
        packetNetworkMap = new EnumMap<>(EligibleCommunicationEntity.class);
        packetNetworkMap.put(SERVER, serverBoundNetwork);
        packetNetworkMap.put(CLIENT, clientBoundNetwork);
        this.packetNetworkManager = new CommunicationNetworkSubscriptionManager(packetNetworkMap);
    }

    @Override
    public void finishSetup() {
        final var serviceId = super.getServiceId();
        final var identificationValidation = ServerPermittedCategoryContentAssociationProvider
                .createRegularServerPacketContentValidator();

        this.validator = new SimplePacketChecker(identificationValidation);

        this.requestBuffer = this.serviceBuffers.registerBuffer(super.getServiceType(), serviceId);
        this.preProcessor = new ContentPreProcessor(this.packetNetworkManager.getSubscriptionsManager(CLIENT), this.requestBuffer);
        this.pheProcessor = ServerPacketHandlingExceptionCreator.getServiceExceptionProcessor();
        super.setReadyState();
    }

    @Override
    public void work() {
        Packet request;

        try {
            request = this.requestBuffer.getPacket();
            if (request != null) {
                processPacket(request);
            }
        } catch (InterruptedException ie) {
            ServiceBase.LOGGER.error("Interrupted while waiting for next Packet.");
            Thread.currentThread().interrupt();
        }
    }

    private void processPacket(final Packet nextPacket) {
        final var validationString = validator.checkPacket(nextPacket);

        if (validationString.isEmpty()) {
            try {
                publishPacket(nextPacket);
            } catch (PacketHandlingException phe) {
                final var errorMessage = "Packet could not be delivered. " + phe.getMessage();
                final var errorResponse = this.pheProcessor
                        .processException(new PacketTransmissionException(errorMessage), nextPacket);

                if (errorResponse != null) {
                    this.requestBuffer.prependPacket(errorResponse);
                }
            }
        } else {
            throw new RuntimeException(validationString.get());
        }
    }

    private void publishPacket(Packet toPublish)
            throws PacketProcessingException {
        final var publishablePacket = this.preProcessor.handleDistributableContent(toPublish);

        if (publishablePacket != null) {
            final var properties = publishablePacket.getPacketProperties();
            final var subscriptionNetwork = this.packetNetworkManager
                    .getSubscriptionsManager(properties.getRecipient().getEntity());

            if (subscriptionNetwork != null) {
                subscriptionNetwork.publish(publishablePacket);
            } else {
                throw new PacketTransmissionException(
                        "No subscription manager found for: " + properties.getRecipient().toString());
            }
        } else {
            throw new PacketTransmissionException("Packet cannot be sent.");
        }
    }

    @Override
    public void breakDown() {
        this.serviceBuffers.deregisterBuffer(getServiceType(), getServiceId(), this.requestBuffer);
        retainPackets();
    }

    private void retainPackets() {
        final var remainingPackets = this.requestBuffer.freezeBuffer();

        for (final var packet : remainingPackets) {
            final var validationString = validator.checkPacket(packet);

            if (validationString.isPresent()) {
                throw new RuntimeException(validationString.get());
            }

            if (packet.getPacketContent() instanceof ExtendedStatusSyncDTO extendedStatusSyncDTO) {
                var clients = this.packetNetworkManager.getSubscriptionsManager(CLIENT).getThreads(CHAT);
                clients.removeIf((client) -> !(extendedStatusSyncDTO.getContactIdSet().contains(client)));
                PacketRetainer.retainExtendedStatus(extendedStatusSyncDTO, clients);
            } else {
                Packet result = PacketRetainer.retainIfResponse(packet);

                if (result != null) {
                    var pheProcessor = ServerPacketHandlingExceptionCreator.getServiceExceptionProcessor();
                    var phe = new PacketProcessingException("Packet could not be delivered.");
                    var errorPacket = pheProcessor.processException(phe, result);
                    result = PacketRetainer.retainIfResponse(errorPacket);

                    if (result != null) {
                        LOGGER.error("Packet discarded: {}", result);
                    }
                }
            }
        }
    }
}
