package de.vsy.server.client_handling.strategy;

import de.vsy.server.client_handling.data_management.HandlerLocalDataManager;
import de.vsy.server.client_handling.data_management.logic.AuthenticationStateControl;
import de.vsy.server.client_management.ClientState;
import de.vsy.server.data.access.PendingClientRegistry;
import de.vsy.server.persistent_data.client_data.PendingPacketDAO;
import de.vsy.server.persistent_data.client_data.PendingType;
import de.vsy.shared_module.packet_management.ClientDataProvider;
import de.vsy.shared_module.packet_management.PacketBuffer;
import de.vsy.shared_module.packet_management.PacketTransmissionCache;
import de.vsy.shared_transmission.packet.Packet;
import de.vsy.shared_utility.async_value_acquisition.TimeBasedValueFetcher;
import de.vsy.shared_utility.logging.ThreadContextRunnable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static de.vsy.shared_module.packet_management.ThreadPacketBufferLabel.HANDLER_BOUND;
import static de.vsy.shared_module.packet_management.ThreadPacketBufferLabel.SERVER_BOUND;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * The Class PendingClientBufferWatcher.
 */
public class PendingClientBufferWatcher extends ThreadContextRunnable {

    private static final long PENDING_END = 25000L;
    private static final int TERMINATION_LATCH_COUNT = 1;
    private static final Logger LOGGER = LogManager.getLogger();
    private final PacketBuffer clientBuffer;
    private final PacketBuffer assignmentBuffer;
    private final PacketTransmissionCache packetCache;
    private final CountDownLatch terminationLatch;
    private final PendingPacketDAO pendingPacketAccessor;
    private final AuthenticationStateControl clientStateControl;
    private final ClientDataProvider localClientData;
    private final PendingClientRegistry pendingRegistry;

    /**
     * Instantiates a new pending client buffer watcher.
     *
     * @param threadDataAccess the thread data access
     */
    public PendingClientBufferWatcher(final HandlerLocalDataManager threadDataAccess, final PendingClientRegistry pendingRegistry) {
        super();
        this.pendingPacketAccessor = threadDataAccess.getLocalClientStateObserverManager()
                .getClientPersistentAccess().getPendingPacketDAO();
        this.localClientData = threadDataAccess.getLocalClientDataProvider();
        this.clientBuffer = threadDataAccess.getHandlerBufferManager()
                .getPacketBuffer(HANDLER_BOUND);
        this.assignmentBuffer = threadDataAccess.getHandlerBufferManager().getPacketBuffer(SERVER_BOUND);
        this.clientStateControl = threadDataAccess.getAuthenticationStateControl();
        this.packetCache = threadDataAccess.getPacketTransmissionCache();
        this.pendingRegistry = pendingRegistry;
        this.terminationLatch = new CountDownLatch(TERMINATION_LATCH_COUNT);
    }

    @Override
    protected void runWithContext() {
        LOGGER.info("{}-PendingClientBufferWatcher initiated.", this.localClientData.getClientId());
        TimeBasedValueFetcher<Boolean> reconnectFlagFetcher;
        ScheduledExecutorService reconnectFlagCheck = Executors.newSingleThreadScheduledExecutor();
        Instant terminationTime = Instant.now().plusMillis(PENDING_END);

        reconnectFlagFetcher = new TimeBasedValueFetcher<>(
                this.clientStateControl::getPersistentReconnectionState, true,
                terminationTime, terminationLatch);
        reconnectFlagCheck.scheduleWithFixedDelay(reconnectFlagFetcher, 20, 50, TimeUnit.MILLISECONDS);
        processIncomingPackets();
        shutdownFlagFetcher(reconnectFlagCheck);
        evaluateReconnectionState(reconnectFlagFetcher);
        this.pendingPacketAccessor.removeFileAccess();
        this.pendingRegistry.removePendingClient(localClientData.getClientId());
        this.clientStateControl.deregisterClient();
        LOGGER.info("{}-PendingClientBufferWatcher ended.", this.localClientData.getClientId());
    }

    private void shutdownFlagFetcher(ScheduledExecutorService reconnectFlagCheck) {
        final boolean flagCheckDown;
        reconnectFlagCheck.shutdownNow();
        try {
            flagCheckDown = reconnectFlagCheck.awaitTermination(5, SECONDS);
            if (flagCheckDown) {
                LOGGER.trace("ReconnectionFlagCheck shutdown regularly.");
            } else {
                LOGGER.error(
                        "ReconnectionFlagCheck shutdown unexpectedly took more than 5 seconds and might.");
            }
        } catch (InterruptedException ie) {
            LOGGER.error("Interrupted while waiting for ReconnectionFlagChecker to terminate.");
        }
    }

    /**
     * Save incoming Packet.
     */
    private void processIncomingPackets() {
        this.pendingPacketAccessor.createAccess(String.valueOf(this.localClientData.getClientId()));

        while (this.terminationLatch.getCount() == TERMINATION_LATCH_COUNT) {
            try {
                Packet currentPacket = clientBuffer.getPacket(250);
                if (currentPacket != null) {
                    this.pendingPacketAccessor.appendPendingPacket(PendingType.PROCESSOR_BOUND,
                            currentPacket);
                    //TODO Hier Antwortpaket versenden, dass ueber mogliche verspaetungen informiert
                    // SimpleInformationDTO
                }
            } catch (InterruptedException ie) {
                LOGGER.warn("Interrupted while waiting for a Packet. Packets may not be saved.");
            }
        }
    }

    private void evaluateReconnectionState(TimeBasedValueFetcher<Boolean> reconnectFlagFetcher) {
        final boolean hasReconnected = reconnectFlagFetcher.getFetchedValue();

        if (hasReconnected) {
            LOGGER.info("Client reconnected.");
            this.clientStateControl.changePersistentPendingState(false);
        } else {
            LOGGER.info("Client timed out, will now be logged out.");
            removeVolatilePendingPackets();
            this.clientStateControl.appendSynchronizationRemovalPacketPerState();
            this.clientStateControl.changePersistentClientState(ClientState.AUTHENTICATED, false);
            this.packetCache.transmitPackets(this.assignmentBuffer::appendPacket);
        }
    }

    private void removeVolatilePendingPackets() {
        for (final var pendingDirection : PendingType.values()) {
            Map<String, Packet> remainingPackets = new LinkedHashMap<>();
            var allPackets = this.pendingPacketAccessor.readPendingPackets(pendingDirection);

            if (!allPackets.isEmpty()) {

                for (final var currentPacketEntry : allPackets.entrySet()) {
                    final var currentPacket = currentPacketEntry.getValue();

                    if (!(VolatilePacketIdentifier.checkPacketVolatility(currentPacket))) {
                        remainingPackets.put(currentPacketEntry.getKey(), currentPacket);
                    }
                }
                this.pendingPacketAccessor.setPendingPackets(pendingDirection, remainingPackets);
            }
        }
    }
}
