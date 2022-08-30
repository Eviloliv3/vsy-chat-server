package de.vsy.server.service.error;

import de.vsy.server.server.data.AbstractPacketCategorySubscriptionManager;
import de.vsy.shared_module.shared_module.packet_management.PacketBuffer;
import de.vsy.server.persistent_data.client_data.PendingPacketDAO;
import de.vsy.server.persistent_data.client_data.PendingType;
import de.vsy.server.server.data.access.ErrorHandlingServiceDataProvider;
import de.vsy.server.service.Service;
import de.vsy.server.service.ServiceBase;
import de.vsy.server.service.ServiceData;
import de.vsy.server.service.ServiceData.ServiceDataBuilder;
import de.vsy.server.service.ServiceData.ServiceResponseDirection;
import de.vsy.server.service.ServicePacketBufferManager;
import de.vsy.server.service.packet_logic.ErrorPacketProcessorFactory;
import de.vsy.server.service.packet_logic.processor.ServicePacketProcessor;
import de.vsy.shared_transmission.shared_transmission.packet.Packet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static de.vsy.shared_transmission.shared_transmission.packet.property.packet_category.PacketCategory.ERROR;
import static java.util.Arrays.asList;

/** The Class ErrorMessageHandlingService. */
public
class ErrorMessageHandlingService extends ServiceBase {

    private static final ServiceData SERVICE_SPECIFICATIONS;
    private static final Logger LOGGER = LogManager.getLogger();
    private final ServicePacketProcessor processor;
    private final AbstractPacketCategorySubscriptionManager serverBoundNetwork;
    private final ServicePacketBufferManager serverBuffers;
    private PacketBuffer inputBuffer;

    static {
        SERVICE_SPECIFICATIONS = ServiceDataBuilder.create()
                                                   .withType(
                                                           Service.TYPE.ERROR_HANDLER)
                                                   .withName("ErrorHandlingService")
                                                   .withDirection(
                                                           ServiceResponseDirection.INBOUND,
                                                           Service.TYPE.REQUEST_ROUTER)
                                                   .withDirection(
                                                           ServiceResponseDirection.OUTBOUND,
                                                           Service.TYPE.SERVER_TRANSFER)
                                                   .build();
    }

    /**
     * Instantiates a new error message handling service.
     *
     * @param serviceDataModel the dataManagement manager
     */
    public
    ErrorMessageHandlingService (
            final ErrorHandlingServiceDataProvider serviceDataModel) {
        super(SERVICE_SPECIFICATIONS,
              serviceDataModel.getServicePacketBufferManager());
        this.serverBuffers = serviceDataModel.getServicePacketBufferManager();
        this.serverBoundNetwork = serviceDataModel.getServiceSubscriptionManager();
        this.processor = new ServicePacketProcessor(
                new ErrorPacketProcessorFactory(serviceDataModel));
    }

    @Override
    public
    void finishSetup () {
        final var serviceId = super.getServiceId();
        this.inputBuffer = serverBuffers.registerBuffer(
                SERVICE_SPECIFICATIONS.getServiceType(), serviceId);
        serverBoundNetwork.subscribe(ERROR, serviceId, inputBuffer);

        setReadyState();
    }

    @Override
    public
    boolean interruptionConditionNotMet () {
        return !Thread.currentThread().isInterrupted();
    }

    @Override
    public
    void work () {
        Packet input = null;

        try {
            input = inputBuffer.getPacket();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            LOGGER.error("Beim Holen des naechsten Pakets unterbrochen.\n{}",
                         asList(ie.getStackTrace()));
        }

        if (input != null) {
            final var responseMap = this.processor.processPacket(input);
            super.dispatchResponsePacketMap(responseMap);
        }
    }

    @Override
    public
    void breakDown () {
        var reinterrupt = false;
        serverBuffers.deregisterBuffer(getServiceType(), getServiceId(),
                                       inputBuffer);

        while (inputBuffer.containsPackets()) {
            Packet input;
            try {
                input = inputBuffer.getPacket();
            } catch (InterruptedException ie) {
                LOGGER.error(
                        "Fehlernachrichtenpuffer wird geleert. Interrupt wird ignoriert");
                reinterrupt = true;
                input = null;
            }

            if (input != null) {
                final var responseMap = this.processor.processPacket(input);
                var response = responseMap.getClientBoundPacket();

                if (response != null) {
                    saveClientBoundError(responseMap.getClientBoundPacket());
                }
                response = responseMap.getServerBoundPacket();

                if (response != null) {
                    LOGGER.error("Serverfehler:\n{}", response);
                }
            }
        }

        if (reinterrupt) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     *
     * @param toPersist     the error packet to persist
     *
     * @throws IllegalStateException    the illegal state exception
     */
    private
    void saveClientBoundError (final Packet toPersist) {
        final var clientDAO = new PendingPacketDAO();
        try {
            clientDAO.createFileAccess(
                    toPersist.getPacketProperties().getRecipientEntity().getEntityId());
        } catch (InterruptedException ie) {
            throw new IllegalStateException(ie);
        }

        if (clientDAO.persistPacket(PendingType.PROCESSOR_BOUND, toPersist)) {
            LOGGER.error("Fehler beim Speichern von: {}", toPersist);
        }
    }
}
