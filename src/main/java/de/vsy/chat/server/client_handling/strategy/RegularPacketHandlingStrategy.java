package de.vsy.chat.server.client_handling.strategy;

import de.vsy.chat.shared_module.packet_exception.PacketHandlingException;
import de.vsy.chat.shared_module.packet_management.ThreadPacketBufferLabel;
import de.vsy.chat.shared_module.packet_management.ThreadPacketBufferManager;
import de.vsy.chat.shared_module.packet_processing.PacketProcessor;
import de.vsy.chat.shared_module.packet_transmission.ConnectionThreadControl;
import de.vsy.chat.shared_module.packet_validation.SemanticPacketValidator;
import de.vsy.chat.shared_module.packet_validation.SimplePacketChecker;
import de.vsy.chat.shared_module.packet_validation.content_validation.ClientPacketTypeValidationCreator;
import de.vsy.chat.server.client_handling.data_management.HandlerLocalDataManager;
import de.vsy.chat.server.client_handling.data_management.bean.LocalClientDataProvider;
import de.vsy.chat.server.client_handling.data_management.bean.LocalClientStateProvider;
import de.vsy.chat.server.client_handling.packet_processing.content_processor_provisioning.StandardProcessorFactoryProvider;
import de.vsy.chat.server.client_handling.packet_processing.processor.*;
import de.vsy.chat.server.client_handling.persistent_data_access.ClientPersistentDataAccessProvider;
import de.vsy.chat.server.persistent_data.client_data.PendingType;
import de.vsy.chat.server.server_packet.dispatching.ClientPacketDispatcher;
import de.vsy.chat.server.server_packet.dispatching.MultiplePacketDispatcher;
import de.vsy.chat.server.server_packet.dispatching.PacketTransmissionCache;
import de.vsy.chat.server.server_packet.packet_creation.ResultingPacketContentHandler;
import de.vsy.chat.server.server_packet.packet_validation.ServerPacketTypeValidationCreator;
import de.vsy.chat.shared_transmission.packet.Packet;
import de.vsy.chat.shared_transmission.packet.content.error.ErrorDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/**
 * Contains Packetprocessing strategies during live connection. Pending Packet are
 * added to the respective Buffers. Neither remaining Packet nor pending Packet are
 * dealt with.
 */
public
class RegularPacketHandlingStrategy implements PacketHandlingStrategy {

    private static final Logger LOGGER = LogManager.getLogger();
    private final LocalClientStateProvider clientStateAccess;
    private final LocalClientDataProvider clientDataProvider;
    private final ConnectionThreadControl connectionControl;
    private final ResultingPacketCreator packetCreator;
    private final ResultingPacketContentHandler contentHandler;
    private final PacketTransmissionCache packetsToDispatch;
    private final MultiplePacketDispatcher dispatcher;
    private final ClientPersistentDataAccessProvider persistentData;
    private final ThreadPacketBufferManager threadLocalBuffers;
    private PacketProcessor processor;

    /**
     * Instantiates a new standard buffer handling strategy.
     *
     * @param threadDataAccess the thread bean access
     * @param connectionControl the connection control
     */
    public
    RegularPacketHandlingStrategy (final HandlerLocalDataManager threadDataAccess,
                                   final ConnectionThreadControl connectionControl) {
        this.packetCreator = threadDataAccess.getResultingPacketCreator();
        this.packetsToDispatch = threadDataAccess.getPacketTransmissionCache();
        this.contentHandler = threadDataAccess.getResultingPacketContentHandler();
        this.clientStateAccess = threadDataAccess.getLocalClientStateProvider();
        this.clientDataProvider = threadDataAccess.getLocalClientDataProvider();
        this.connectionControl = connectionControl;
        this.threadLocalBuffers = threadDataAccess.getHandlerBufferManager();
        this.persistentData = threadDataAccess.getLocalClientStateDependentLogicProvider()
                                              .getClientPersistentAccess();

        this.dispatcher = new ClientPacketDispatcher(
                threadDataAccess.getLocalClientDataProvider(),
                this.threadLocalBuffers);
        setupProcessor(threadDataAccess);
    }

    private
    void setupProcessor (final HandlerLocalDataManager threadDataAccess) {
        final var processorLink = new ClientPacketProcessorLink(
                new PacketProcessorManager(threadDataAccess,
                                           new StandardProcessorFactoryProvider()));
        final var contextCheckLink = new PacketContextCheckLink(processorLink,
                                                                threadDataAccess.getLocalClientStateDependentLogicProvider()
                                                                                .getPermittedPacketCategoryCheck());
        this.processor = new PacketSyntaxCheckLink(contextCheckLink,
                                                   new SimplePacketChecker(
                                                           setupValidator()));
    }

    private
    SemanticPacketValidator setupValidator () {
        SemanticPacketValidator packetValidator = ClientPacketTypeValidationCreator.createRegularPacketContentValidator();
        var regularServerValidation = ServerPacketTypeValidationCreator.setupRegularServerPacketContentValidation();

        for (var categoryValidator : regularServerValidation.entrySet()) {
            packetValidator.addCategoryAssociations(categoryValidator.getKey(),
                                                    categoryValidator.getValue());
        }
        return packetValidator;
    }

    @Override
    public
    void administerStrategy () {

        while (this.connectionControl.connectionIsLive() &&
               !Thread.currentThread().isInterrupted()) {
            processStateConformPackets();

            if (this.clientStateAccess.clientStateHasRisen()) {
                appendPendingPackets();
            }
        }
    }

    /** Process state conform Packet. */
    private
    void processStateConformPackets () {
        var stateChanged = false;
        var buffer = this.threadLocalBuffers.getPacketBuffer(
                ThreadPacketBufferLabel.HANDLER_BOUND);

        while (!stateChanged && this.connectionControl.connectionIsLive()) {
            Packet input;

            try {
                input = buffer.getPacket();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                LOGGER.error("Beim Holen des naechsten Pakets unterbrochen.");
                break;
            }

            if (input != null) {
                this.packetCreator.changeCurrentRequest(input);

                try {
                    this.processor.processPacket(input);
                } catch (final PacketHandlingException phe) {
                    final var errorContent = new ErrorDTO(phe.getMessage(), input);
                    this.contentHandler.setError(errorContent);
                }
                this.packetsToDispatch.transmitPackets(this.dispatcher);

                stateChanged = this.clientStateAccess.clientStateHasChanged();
            }
        }
    }

    /** Process pending Packet. */
    private
    void appendPendingPackets () {
        final var strategies = getPendingPacketStrategies();
        final var pendingPacketProvider = this.persistentData.getPendingPacketDAO();
        final var allPendingPackets = pendingPacketProvider.readAllPendingPackets();

        for (final var currentPendingPacketMap : allPendingPackets.entrySet()) {
            final var currentClassification = currentPendingPacketMap.getKey();
            final var pendingMap = currentPendingPacketMap.getValue();

            if (strategies.containsKey(currentClassification)) {
                final var pendingStrategy = strategies.get(currentClassification);
                List<String> handledPackets = new ArrayList<>();

                for (final var currentPacket : pendingMap.entrySet()) {
                    final var packetHandled = pendingStrategy.handlePacket(
                            currentPacket.getValue());

                    if (packetHandled) {
                        handledPackets.add(currentPacket.getKey());
                    }
                }

                for (final var currentHash : handledPackets) {
                    pendingMap.remove(currentHash);
                }
            } else {
                LOGGER.info(
                        "Es gibt keine verpassten Pakete der Klassifikation \"{}\" zu verarbeiten.",
                        currentClassification);
            }

            pendingPacketProvider.setPendingPackets(currentClassification,
                                                    pendingMap);
        }
    }

    /**
     * Gets the pending Packetstrategies.
     *
     * @return the pending Packetstrategies
     */
    private
    EnumMap<PendingType, PendingPacketHandlingStrategy> getPendingPacketStrategies () {
        final var strategies = new EnumMap<PendingType, PendingPacketHandlingStrategy>(
                PendingType.class);
        strategies.put(PendingType.CLIENT_BOUND, getClientBoundStrategy());
        strategies.put(PendingType.PROCESSOR_BOUND, getProcessingPacketStrategy());
        return strategies;
    }

    /**
     * Gets the forwardable Packetstrategy.
     *
     * @return the forwardable Packetstrategy
     */
    private
    PendingPacketHandlingStrategy getClientBoundStrategy () {
        final var clientBuffer = this.threadLocalBuffers.getPacketBuffer(
                ThreadPacketBufferLabel.OUTSIDE_BOUND);

        return clientBuffer::appendPacket;
    }

    /**
     * Gets the processing Packetstrategy.
     *
     * @return the processing Packetstrategy
     */
    private
    PendingPacketHandlingStrategy getProcessingPacketStrategy () {
        final var clientBuffer = this.threadLocalBuffers.getPacketBuffer(
                ThreadPacketBufferLabel.HANDLER_BOUND);

        return clientBuffer::prependPacket;
    }
}
