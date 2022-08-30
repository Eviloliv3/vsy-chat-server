/*
 *
 */
package de.vsy.server.service.request;

import de.vsy.server.server.data.AbstractPacketCategorySubscriptionManager;
import de.vsy.shared_module.shared_module.exception_processing.PacketHandlingExceptionProcessor;
import de.vsy.shared_module.shared_module.packet_exception.PacketHandlingException;
import de.vsy.shared_module.shared_module.packet_exception.PacketProcessingException;
import de.vsy.shared_module.shared_module.packet_exception.PacketTransmissionException;
import de.vsy.shared_module.shared_module.packet_exception.PacketValidationException;
import de.vsy.shared_module.shared_module.packet_management.PacketBuffer;
import de.vsy.shared_module.shared_module.packet_validation.PacketCheck;
import de.vsy.shared_module.shared_module.packet_validation.SimplePacketChecker;
import de.vsy.server.exception_processing.ServerPacketHandlingExceptionCreator;
import de.vsy.server.server.data.access.PacketAssignmentServiceDataProvider;
import de.vsy.server.server_packet.packet_validation.ServerPacketTypeValidationCreator;
import de.vsy.server.service.*;
import de.vsy.server.service.ServiceData.ServiceDataBuilder;
import de.vsy.server.service.ServiceData.ServiceResponseDirection;
import de.vsy.shared_transmission.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.shared_transmission.packet.property.communicator.EligibleCommunicationEntity;

import java.util.EnumMap;
import java.util.Map;

import static de.vsy.shared_transmission.shared_transmission.packet.property.communicator.EligibleCommunicationEntity.CLIENT;
import static de.vsy.shared_transmission.shared_transmission.packet.property.communicator.EligibleCommunicationEntity.SERVER;

/** Service assigning clients' requests to the appropriate service. */
public
class PacketAssignmentService extends ServiceBase {

    private static final ServiceData SERVICE_SPECIFICATIONS;
    private final ServicePacketBufferManager serviceBuffers;
    private PacketHandlingExceptionProcessor pheProcessor;
    private PublishablePacketCreator preProcessor;
    private CommunicationNetworkSubscriptionManager packetNetworkManager;
    private PacketBuffer requestBuffer;
    private PacketCheck validator;

    static {
        SERVICE_SPECIFICATIONS = ServiceDataBuilder.create()
                                                   .withType(
                                                           Service.TYPE.REQUEST_ROUTER)
                                                   .withName(
                                                           "PacketAssignmentService")
                                                   .withDirection(
                                                           ServiceResponseDirection.INBOUND,
                                                           Service.TYPE.REQUEST_ROUTER)
                                                   .withDirection(
                                                           ServiceResponseDirection.OUTBOUND,
                                                           Service.TYPE.SERVER_TRANSFER)
                                                   .build();
    }

    /**
     * Instantiates a new client request assignment service.
     *
     * @param serviceDataModel the service dataManagement model
     */
    public
    PacketAssignmentService (
            final PacketAssignmentServiceDataProvider serviceDataModel) {
        super(SERVICE_SPECIFICATIONS,
              serviceDataModel.getServicePacketBufferManager());
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
    private
    void setupPacketNetworkManager (
            final AbstractPacketCategorySubscriptionManager serverBoundNetwork,
            final AbstractPacketCategorySubscriptionManager clientBoundNetwork) {
        Map<EligibleCommunicationEntity, AbstractPacketCategorySubscriptionManager> packetNetworMap;

        packetNetworMap = new EnumMap<>(EligibleCommunicationEntity.class);

        packetNetworMap.put(SERVER, serverBoundNetwork);
        packetNetworMap.put(CLIENT, clientBoundNetwork);

        this.packetNetworkManager = new CommunicationNetworkSubscriptionManager(
                packetNetworMap);
    }

    @Override
    public
    void finishSetup () {
        final var serviceId = super.getServiceId();
        final var identificatorValidation = ServerPacketTypeValidationCreator.createRegularServerPacketContentValidator();

        this.validator = new SimplePacketChecker(identificatorValidation);

        this.requestBuffer = this.serviceBuffers.registerBuffer(
                super.getServiceType(), serviceId);
        this.preProcessor = new ContentPreProcessor(
                this.packetNetworkManager.getSubscriptionsManager(CLIENT),
                this.requestBuffer);
        this.pheProcessor = ServerPacketHandlingExceptionCreator.getServiceExceptionProcessor();
        super.setReadyState();
    }

    @Override
    public
    boolean interruptionConditionNotMet () {
        return !Thread.currentThread().isInterrupted();
    }

    @Override
    public
    void work () {
        Packet request;
        String validationString;

        try {
            request = this.requestBuffer.getPacket();
        } catch (InterruptedException ie) {
            this.getServiceLogger().info("");
            Thread.currentThread().interrupt();
            return;
        }

        if (request != null) {

            validationString = validator.checkPacket(request);

            if (validationString == null) {
                try {
                    publishPacket(request);
                } catch (PacketHandlingException phe) {
                    final var errorMessage =
                            "Das Paket wurde nicht zugestellt. " + phe.getMessage();
                    final var errorResponse = this.pheProcessor.processException(
                            new PacketTransmissionException(errorMessage), request);
                    this.requestBuffer.prependPacket(errorResponse);
                }
            } else {
                this.getServiceLogger().error(validationString);
            }
        }
    }

    private
    void publishPacket (Packet toPublish)
    throws PacketProcessingException, PacketValidationException {
        final var publishablePacket = this.preProcessor.createPublishablePacket(
                toPublish);

        if (publishablePacket != null) {
            final var properties = publishablePacket.getPacketProperties();
            final var subscriptionNetwork = this.packetNetworkManager.getSubscriptionsManager(
                    properties.getRecipientEntity().getEntity());

            if (subscriptionNetwork != null) {
                subscriptionNetwork.publish(publishablePacket);
            } else {
                throw new PacketTransmissionException(
                        "Kein Abonnenten-Netz gefunden für: " +
                        properties.getRecipientEntity().toString());
            }
        } else {
            throw new PacketTransmissionException(
                    "Paket nicht zu senden. " + toPublish);
        }
    }

    @Override
    public
    void breakDown () {
        this.serviceBuffers.deregisterBuffer(getServiceType(), getServiceId(),
                                             this.requestBuffer);
    }
}
