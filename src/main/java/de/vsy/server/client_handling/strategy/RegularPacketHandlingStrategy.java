package de.vsy.server.client_handling.strategy;

import de.vsy.server.client_handling.data_management.HandlerLocalDataManager;
import de.vsy.server.client_handling.data_management.bean.LocalClientStateProvider;
import de.vsy.server.client_handling.packet_processing.content_processor_provisioning.StandardProcessorFactoryProvider;
import de.vsy.server.client_handling.packet_processing.processor.ClientPacketProcessorLink;
import de.vsy.server.client_handling.packet_processing.processor.PacketContextCheckLink;
import de.vsy.server.client_handling.packet_processing.processor.PacketProcessorManager;
import de.vsy.server.client_handling.packet_processing.processor.ResultingPacketCreator;
import de.vsy.server.server_packet.dispatching.ClientPacketDispatcher;
import de.vsy.server.server_packet.packet_creation.ResultingPacketContentHandler;
import de.vsy.server.server_packet.packet_validation.ServerPermittedCategoryContentAssociationProvider;
import de.vsy.shared_module.packet_exception.PacketHandlingException;
import de.vsy.shared_module.packet_management.MultiplePacketDispatcher;
import de.vsy.shared_module.packet_management.PacketTransmissionCache;
import de.vsy.shared_module.packet_management.ThreadPacketBufferLabel;
import de.vsy.shared_module.packet_management.ThreadPacketBufferManager;
import de.vsy.shared_module.packet_processing.PacketProcessor;
import de.vsy.shared_module.packet_processing.PacketSyntaxCheckLink;
import de.vsy.shared_module.packet_transmission.ConnectionThreadControl;
import de.vsy.shared_module.packet_validation.SemanticPacketValidator;
import de.vsy.shared_module.packet_validation.SimplePacketChecker;
import de.vsy.shared_module.packet_validation.content_validation.ClientPacketSemanticsValidationCreator;
import de.vsy.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.packet.content.error.ErrorDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static de.vsy.shared_module.packet_management.ThreadPacketBufferLabel.OUTSIDE_BOUND;
import static de.vsy.shared_module.packet_management.ThreadPacketBufferLabel.SERVER_BOUND;

/**
 * Contains Packet processing strategies during live connection. Pending Packet are added to the
 * respective Buffers. Neither remaining Packet nor pending Packet are dealt with.
 */
public class RegularPacketHandlingStrategy implements PacketHandlingStrategy {

    private static final Logger LOGGER = LogManager.getLogger();
    private final LocalClientStateProvider clientStateAccess;
    private final ConnectionThreadControl connectionControl;
    private final ResultingPacketCreator packetCreator;
    private final ResultingPacketContentHandler contentHandler;
    private final PacketTransmissionCache packetsToDispatch;
    private final MultiplePacketDispatcher dispatcher;
    private final ThreadPacketBufferManager threadLocalBuffers;
    private final StateDependentPacketRetriever packetRetriever;
    private PacketProcessor processor;

    /**
     * Instantiates a new standard buffer handling strategy.
     *
     * @param threadDataAccess  the thread bean access
     * @param connectionControl the connection control
     */
    public RegularPacketHandlingStrategy(final HandlerLocalDataManager threadDataAccess,
                                         final ConnectionThreadControl connectionControl) {
        this.packetCreator = threadDataAccess.getResultingPacketCreator();
        this.packetsToDispatch = threadDataAccess.getPacketTransmissionCache();
        this.contentHandler = threadDataAccess.getResultingPacketContentHandler();
        this.clientStateAccess = threadDataAccess.getLocalClientStateProvider();
        this.connectionControl = connectionControl;
        this.threadLocalBuffers = threadDataAccess.getHandlerBufferManager();

        this.dispatcher = new ClientPacketDispatcher(threadDataAccess.getLocalClientDataProvider(),
                this.threadLocalBuffers.getPacketBuffer(OUTSIDE_BOUND),
                this.threadLocalBuffers.getPacketBuffer(SERVER_BOUND));
        this.packetRetriever = threadDataAccess.getStateDependentPacketRetriever();
        setupProcessor(threadDataAccess);
    }

    private void setupProcessor(final HandlerLocalDataManager threadDataAccess) {
        final var processorLink = new ClientPacketProcessorLink(
                new PacketProcessorManager(threadDataAccess, new StandardProcessorFactoryProvider()));
        final var contextCheckLink = new PacketContextCheckLink(processorLink,
                threadDataAccess.getLocalClientStateObserverManager()
                        .getPermittedPacketCategoryCheck());
        this.processor = new PacketSyntaxCheckLink(contextCheckLink,
                new SimplePacketChecker(setupValidator()));
    }

    private SemanticPacketValidator setupValidator() {
        SemanticPacketValidator packetValidator = ClientPacketSemanticsValidationCreator.createSemanticValidator();
        var serverPacketValidation = ServerPermittedCategoryContentAssociationProvider.setupRegularServerPacketContentValidation();

        for (var associationSet : serverPacketValidation.entrySet()) {
            packetValidator.addCategoryAssociations(associationSet.getKey(), associationSet.getValue());
        }
        return packetValidator;
    }

    @Override
    public void administerStrategy() {

        while (this.connectionControl.connectionIsLive() && !Thread.currentThread().isInterrupted()) {
            processStateConformPackets();
        }
    }

    /**
     * Process state conform Packet.
     */
    private void processStateConformPackets() {
        var stateChanged = false;
        var buffer = this.threadLocalBuffers.getPacketBuffer(ThreadPacketBufferLabel.HANDLER_BOUND);

        while (!stateChanged && this.connectionControl.connectionIsLive()) {
            Packet input;

            try {
                input = buffer.getPacket(250);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                LOGGER.error("Interrupted while waiting for next Packet.");
                break;
            }

            if (input != null) {
                this.packetCreator.setCurrentPacket(input);

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
        if (this.clientStateAccess.clientStateHasRisen()) {
            this.packetRetriever.getPendingPackets();
        }
    }
}
