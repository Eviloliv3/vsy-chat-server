package de.vsy.server.service.inter_server;

import de.vsy.server.client_handling.data_management.PacketRetainer;
import de.vsy.server.data.PacketCategorySubscriptionManager;
import de.vsy.server.data.access.ServerCommunicationServiceDataProvider;
import de.vsy.server.data.socketConnection.RemoteServerConnectionData;
import de.vsy.server.exception_processing.ServerPacketHandlingExceptionCreator;
import de.vsy.server.persistent_data.client_data.PendingPacketDAO;
import de.vsy.server.persistent_data.server_data.temporal.LiveClientStateDAO;
import de.vsy.server.server_packet.packet_validation.ServerPermittedCategoryContentAssociationProvider;
import de.vsy.server.service.Service;
import de.vsy.server.service.ServicePacketBufferManager;
import de.vsy.server.service.packet_logic.processor.InterServerSubstitutePacketProcessorLink;
import de.vsy.shared_module.packet_exception.PacketHandlingException;
import de.vsy.shared_module.packet_exception.PacketProcessingException;
import de.vsy.shared_module.packet_management.PacketBuffer;
import de.vsy.shared_module.packet_processing.PacketProcessor;
import de.vsy.shared_module.packet_processing.PacketSyntaxCheckLink;
import de.vsy.shared_module.packet_validation.SimplePacketChecker;
import de.vsy.shared_module.thread_manipulation.ProcessingInterruptProvider;
import de.vsy.shared_transmission.packet.Packet;
import de.vsy.shared_utility.logging.ThreadContextRunnable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static de.vsy.shared_utility.standard_value.ThreadContextValues.LOG_FILE_CONTEXT_KEY;
import static java.lang.Thread.interrupted;

/**
 * Service checking for pending PacketQueues that have outlived their ttl, dissolving them if
 * necessary.
 */
public class InterServerSubstituteService extends ThreadContextRunnable implements
        ClientReconnectionHandler {

    private static final long PENDING_END = 25000L;
    private static final AtomicInteger SERVICE_COUNT = new AtomicInteger(1);
    private static final Logger LOGGER = LogManager.getLogger();
    private final int serviceId;
    private final Timer reconnectionStateWatcher;
    private final Map<Integer, PendingPacketDAO> clientPersistenceAccessManagers;
    private final LiveClientStateDAO clientStateProvider;
    private final PacketBuffer interServerBuffer;
    private final RemoteServerConnectionData remoteServerConnection;
    private final ServicePacketBufferManager serviceBuffers;
    private final PacketCategorySubscriptionManager clientSubscriptions;
    private final RemoteClientDisconnector clientDisconnector;
    private PacketProcessor processor;
    private ProcessingInterruptProvider shutdownCondition;
    private PacketBuffer requestBuffer;
    private volatile boolean allClientsReconnected;

    /**
     * Instantiates a new server failed client watcher.
     *
     * @param serviceDataAccess      the service dataManagement access
     * @param remoteServerConnection the remote server connection
     * @param interServerBuffer      the inter server buffer
     */
    public InterServerSubstituteService(
            final ServerCommunicationServiceDataProvider serviceDataAccess,
            final RemoteServerConnectionData remoteServerConnection,
            final PacketBuffer interServerBuffer) {

        this.serviceId = SERVICE_COUNT.incrementAndGet();
        this.reconnectionStateWatcher = new Timer("ClientReconnectionStateWatcher-" + serviceId);
        this.clientPersistenceAccessManagers = new HashMap<>();
        this.clientSubscriptions = serviceDataAccess.getClientSubscriptionManager();
        this.remoteServerConnection = remoteServerConnection;
        this.serviceBuffers = serviceDataAccess.getServicePacketBufferManager();
        this.clientStateProvider = serviceDataAccess.getLiveClientStateDAO();
        this.interServerBuffer = interServerBuffer;
        this.clientDisconnector = new RemoteClientDisconnector(this.interServerBuffer,
                this.serviceBuffers,
                serviceDataAccess.getCommunicatorDataAccessor(), this.clientStateProvider,
                serviceDataAccess.getClientSubscriptionManager());
        this.allClientsReconnected = false;
    }

    @Override
    public void runWithContext() {
        if (finishSetup()) {
            LOGGER.info("{} started.", ThreadContext.get(LOG_FILE_CONTEXT_KEY));

            while (this.shutdownCondition.conditionNotMet()) {
                processPacket();
                //TODO Hier Antwortpaket versenden, dass ueber mogliche verspaetungen informiert
                // SimpleInformationDTO
            }
            this.reconnectionStateWatcher.cancel();
            this.reconnectionStateWatcher.purge();

            if (!(this.allClientsReconnected)) {
                this.clientDisconnector.disconnectRemainingClients(this.clientPersistenceAccessManagers);
                clearBuffer();
            }
        }
        this.serviceBuffers.deregisterBuffer(Service.TYPE.SERVER_TRANSFER, remoteServerConnection.getServerId(),
                this.interServerBuffer);
        LOGGER.info("{} stopped.", ThreadContext.get(LOG_FILE_CONTEXT_KEY));
    }

    /**
     * Finish setup.
     */
    private boolean finishSetup() {
        boolean substituteSetup = false;
        Instant stopTime;
        List<Integer> pendingClientIds;
        ThreadContext.put(LOG_FILE_CONTEXT_KEY, "InterServerSubstitute-" + this.serviceId + "-" + Instant.now());

        pendingClientIds = getPendingClientIds();

        if (!(pendingClientIds.isEmpty())) {
            preparePendingClients(pendingClientIds);

            this.requestBuffer = this.serviceBuffers.getRandomBuffer(Service.TYPE.REQUEST_ROUTER);
            this.processor = new PacketSyntaxCheckLink(
                    new InterServerSubstitutePacketProcessorLink(this.clientPersistenceAccessManagers,
                            this.requestBuffer),
                    new SimplePacketChecker(
                            ServerPermittedCategoryContentAssociationProvider.createRegularServerPacketContentValidator()));
            this.reconnectionStateWatcher.scheduleAtFixedRate(
                    new ClientReconnectionStateWatcher(this.clientStateProvider, pendingClientIds, this), 100, 100);
            stopTime = Instant.now().plusMillis(PENDING_END);
            this.shutdownCondition = () -> !this.allClientsReconnected && !(Instant.now().isAfter(stopTime));
            substituteSetup = true;
        } else {
            LOGGER.info("No remote clients for server {} specified.",
                    this.remoteServerConnection.getServerId());
        }
        return substituteSetup;
    }

    /**
     * Process Packet
     */
    private void processPacket() {
        Packet input = null;

        try {
            input = this.interServerBuffer.getPacket(250);

            if (input != null) {
                processor.processPacket(input);
            }
        } catch (final PacketHandlingException phe) {
            var errorResponse = ServerPacketHandlingExceptionCreator.getServiceExceptionProcessor()
                    .processException(phe, input);
            if (errorResponse != null) {
                this.requestBuffer.appendPacket(errorResponse);
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            LOGGER.error(
                    "InterServerSubstituteService interrupted while waiting for next Packet.");
        }
    }

    /**
     * Clear and remove buffer.
     */
    private void clearBuffer() {
        var reinterrupt = interrupted();
        final var remainingPackets = this.interServerBuffer.freezeBuffer();

        for (final var packet : remainingPackets) {
            Packet result = PacketRetainer.retainIfResponse(packet);

            if (result != null) {
                var pheProcessor = ServerPacketHandlingExceptionCreator.getServiceExceptionProcessor();
                var phe = new PacketProcessingException("Packet could not be delivered.");
                var errorPacket = pheProcessor.processException(phe, result);

                if (!(reinterrupt)) {
                    this.requestBuffer.appendPacket(errorPacket);
                } else {
                    result = PacketRetainer.retainIfResponse(errorPacket);

                    if (result != null) {
                        LOGGER.error("Packet discarded: {}", result);
                    }
                }
            }
        }

        if (reinterrupt) {
            Thread.currentThread().interrupt();
        }
    }

    private List<Integer> getPendingClientIds() {
        final var remoteClientStateMap = this.clientStateProvider
                .getClientStatesForServer(this.remoteServerConnection.getServerId());
        LOGGER.error("Pending clients found for {}: {}", this.remoteServerConnection.getServerId(), List.of(remoteClientStateMap.keySet()));
        return new ArrayList<>(remoteClientStateMap.keySet());
    }

    /**
     * Pending state globally set and pending packet persistent access acquire  per client.
     *
     * @param pendingClientIds the pending client ids
     */
    private void preparePendingClients(List<Integer> pendingClientIds) {

        for (final var currentClientId : pendingClientIds) {
            this.clientStateProvider.changeClientPendingState(currentClientId, true);
            final var pendingPacketAccessor = new PendingPacketDAO();
            pendingPacketAccessor.createAccess(String.valueOf(currentClientId));
            this.clientPersistenceAccessManagers.put(currentClientId, pendingPacketAccessor);
        }
    }

    @Override
    public void processReconnection(int clientId) {

        if (this.clientPersistenceAccessManagers.containsKey(clientId)) {
            this.clientDisconnector.unsubscribeClient(clientId);
            this.clientStateProvider.changeClientPendingState(clientId, false);
            final var reconnectedClientPersistenceAccess = this.clientPersistenceAccessManagers.remove(
                    clientId);
            reconnectedClientPersistenceAccess.removeFileAccess();
        }
    }

    @Override
    public void stopReconnectingClients() {
        this.reconnectionStateWatcher.cancel();
        this.reconnectionStateWatcher.purge();
        this.allClientsReconnected = true;
    }
}
