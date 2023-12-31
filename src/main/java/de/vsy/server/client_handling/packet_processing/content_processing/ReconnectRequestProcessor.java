package de.vsy.server.client_handling.packet_processing.content_processing;

import de.vsy.server.client_handling.data_management.AuthenticationHandlerDataProvider;
import de.vsy.server.client_handling.data_management.logic.AuthenticationStateControl;
import de.vsy.server.data.access.CommunicatorDataManipulator;
import de.vsy.server.data.access.HandlerAccessManager;
import de.vsy.server.server_packet.packet_creation.ResultingPacketContentHandler;
import de.vsy.shared_module.packet_exception.PacketProcessingException;
import de.vsy.shared_module.packet_processing.ContentProcessor;
import de.vsy.shared_transmission.packet.content.authentication.ReconnectRequestDTO;
import de.vsy.shared_transmission.packet.content.authentication.ReconnectResponseDTO;
import de.vsy.shared_utility.async_value_acquisition.TimeBasedValueFetcher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;

public class ReconnectRequestProcessor implements ContentProcessor<ReconnectRequestDTO> {

    private static final long WAIT_MILLIS_PENDING_WATCHER = 10000L;
    private static final int TERMINATION_LATCH_COUNT = 1;
    private static final Logger LOGGER = LogManager.getLogger();
    private final AuthenticationStateControl clientStateManager;
    private final CommunicatorDataManipulator commPersistManager;
    private final ResultingPacketContentHandler contentHandler;

    /**
     * Instantiates a new reconnect PacketHandler.
     *
     * @param threadDataAccess the thread dataManagement accessLimiter
     */
    public ReconnectRequestProcessor(final AuthenticationHandlerDataProvider threadDataAccess) {
        this.clientStateManager = threadDataAccess.getAuthenticationStateControl();
        this.commPersistManager = HandlerAccessManager.getCommunicatorDataManipulator();
        this.contentHandler = threadDataAccess.getResultingPacketContentHandler();
    }

    @Override
    public void processContent(ReconnectRequestDTO toProcess) throws PacketProcessingException {
        String causeMessage = null;
        var clientData = this.commPersistManager.getCommunicatorData(toProcess.getClientData());

        if (clientData != null) {
            final var persistedClientState = this.clientStateManager.reconnectClient(clientData);

            if (persistedClientState != null) {

                if (this.clientStateManager.changePersistentReconnectionState(true)) {
                    LOGGER.info("Reconnection flag set successfully.");

                    try {
                        //TODO for debugging purposes -> reconnect race
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    if (waitForPendingBufferWatcher()) {
                        LOGGER.info("PendingBufferWatcher terminated.");
                        if (this.clientStateManager.changePersistentClientState(persistedClientState, true)) {
                            this.clientStateManager.changePersistentReconnectionState(false);
                            LOGGER.info("Pending state removed.");
                            this.clientStateManager.appendStateSynchronizationPacket(persistedClientState, true);
                            this.contentHandler.addResponse(new ReconnectResponseDTO(true));
                        } else {
                            this.clientStateManager.deregisterClient();
                            causeMessage = "An error occurred while writing your global login state. Please contact the ChatServer support team.";
                        }
                    } else {
                        this.clientStateManager.deregisterClient();
                        causeMessage =
                                "Reconnection attempt took too much time and was cancelled. Please try logging in.";
                    }
                } else {
                    LOGGER.error("Client state could not be saved. Either of client states could not "
                                    + "be accessed or could not be written. Found client data: {}",
                            clientData);
                    causeMessage = "Reconnect request failed, please try again.";
                    this.clientStateManager.deregisterClient();
                }
            } else {
                this.clientStateManager.deregisterClient();
                causeMessage = "You are not registered as authenticated.";
            }
        } else {
            causeMessage = "There is no account with your credentials.";
        }
        if (causeMessage != null) {
            throw new PacketProcessingException(causeMessage);
        }
    }

    /**
     * Wait for pending buffer watcher.
     *
     * @return pending flag removed
     */
    private boolean waitForPendingBufferWatcher() {
        boolean pendingFlagRemoved;
        TimeBasedValueFetcher<Boolean> pendingFlagFetcher;
        Instant terminationTime = Instant.now().plusMillis(WAIT_MILLIS_PENDING_WATCHER);
        CountDownLatch latch = new CountDownLatch(TERMINATION_LATCH_COUNT);
        ScheduledExecutorService pendingFlagCheck = Executors.newSingleThreadScheduledExecutor();

        pendingFlagFetcher = new TimeBasedValueFetcher<>(this.clientStateManager::getPersistentPendingState,
                false,
                terminationTime, latch);
        pendingFlagCheck.scheduleWithFixedDelay(pendingFlagFetcher, 50, 50, TimeUnit.MILLISECONDS);

        try {
            final boolean pendingFlagCheckDown;
            latch.await();
            pendingFlagCheck.shutdownNow();
            pendingFlagCheckDown = pendingFlagCheck.awaitTermination(1, SECONDS);
            if (pendingFlagCheckDown) {
                LOGGER.trace("PendingFlagCheck shutdown successfully.");
            } else {
                LOGGER.error("PendingFlagCheck shutdown unexpectedly took more than 5 seconds.");
            }
            pendingFlagRemoved = !pendingFlagFetcher.getFetchedValue();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            pendingFlagRemoved = false;
        }
        return pendingFlagRemoved;
    }

}
