/*
 *
 */
package de.vsy.server.client_handling.packet_processing.content_processing;

import de.vsy.server.client_handling.data_management.access_limiter.AuthenticationHandlingDataProvider;
import de.vsy.server.client_handling.data_management.logic.AuthenticationStateControl;
import de.vsy.server.server.client_management.ClientState;
import de.vsy.server.server.data.access.CommunicatorDataManipulator;
import de.vsy.server.server.data.access.HandlerAccessManager;
import de.vsy.server.server_packet.packet_creation.ResultingPacketContentHandler;
import de.vsy.shared_module.shared_module.packet_exception.PacketProcessingException;
import de.vsy.shared_module.shared_module.packet_processing.ContentProcessor;
import de.vsy.shared_transmission.shared_transmission.packet.content.authentication.ReconnectRequestDTO;
import de.vsy.shared_transmission.shared_transmission.packet.content.authentication.ReconnectResponseDTO;
import de.vsy.shared_utility.async_value_acquisition.TimeBasedValueFetcher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static de.vsy.shared_utility.standard_value.ThreadContextValues.LOG_FILE_CONTEXT_KEY;

public
class ReconnectRequestProcessor implements ContentProcessor<ReconnectRequestDTO> {

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
    public
    ReconnectRequestProcessor (
            final AuthenticationHandlingDataProvider threadDataAccess) {

        this.clientStateManager = threadDataAccess.getGlobalAuthenticationStateControl();
        this.commPersistManager = HandlerAccessManager.getCommunicatorDataManipulator();
        this.contentHandler = threadDataAccess.getResultingPacketContentHandler();
    }

    @Override
    public
    void processContent (ReconnectRequestDTO toProcess)
    throws PacketProcessingException {
        String causeMessage = null;
        var clientData = this.commPersistManager.getCommunicatorData(
                toProcess.getClientData());

        if (clientData != null) {
            final var persistedClientState = this.clientStateManager.reconnectClient(clientData);

            if (!(persistedClientState.equals(ClientState.OFFLINE))) {

                if (this.clientStateManager.changeReconnectionState(true)) {

                    if (waitForPendingBufferWatcher()) {
                        if (this.clientStateManager.changePersistentClientState(
                                persistedClientState, true)) {
                            this.clientStateManager.changeReconnectionState(false);
                            this.contentHandler.addResponse(
                                    new ReconnectResponseDTO(true));
                        } else {
                            this.clientStateManager.logoutClient();
                            causeMessage =
                                    "Es ist ein Fehler beim Eintragen Ihres Authentifizierungszustandes " +
                                    "aufgetreten. (Reconnect-global) Bitte " +
                                    "melden Sie dies einem ChatServer-" +
                                    "Mitarbeiter";
                        }
                    } else {
                        this.clientStateManager.logoutClient();
                        causeMessage = "Wiederverbindung dauert zu lange und " +
                                       "wurde abgebrochen. Sie müssen sich " +
                                       "erneut authentifizieren.";
                    }
                } else {
                    LOGGER.error("Klientenzustand konnte nicht persistent " +
                                 "gesichert werden. Entweder konnte kein Zugriff " +
                                 "auf Klientenzustaende erlangt oder der " +
                                 "Klientenzustand nicht erfolgreich geschrieben " +
                                 "werden. Gefundene Klientendaten: {}", clientData);
                    causeMessage = "Sie sind entweder von einem anderen Gerät aus " +
                                   "verbunden oder es wird bereits ein " +
                                   "Wiederverbindungsversuch von einem anderen " +
                                   "Gerät aus unternommen.";
                    this.clientStateManager.logoutClient();
                }
            } else {
                this.clientStateManager.logoutClient();
                causeMessage = "Sie sind nicht als authentifiziert registriert.";
            }
        } else {
            causeMessage = "Es existiert kein Account mit den von Ihnen angegebenen Daten.";
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
    private
    boolean waitForPendingBufferWatcher () {
        boolean pendingFlagRemoved;
        TimeBasedValueFetcher<Boolean> pendingFlagFetcher;
        Instant terminationTime = Instant.now()
                                         .plusMillis(WAIT_MILLIS_PENDING_WATCHER);
        CountDownLatch latch = new CountDownLatch(TERMINATION_LATCH_COUNT);
        ScheduledExecutorService pendingFlagCheck = Executors.newSingleThreadScheduledExecutor();

        pendingFlagFetcher = new TimeBasedValueFetcher<>(
                this.clientStateManager::getPendingState, false, terminationTime,
                latch);
        pendingFlagCheck.scheduleWithFixedDelay(pendingFlagFetcher, 50, 30,
                                                TimeUnit.MILLISECONDS);

        try {
            latch.await();
            pendingFlagCheck.shutdownNow();
            pendingFlagRemoved = !pendingFlagFetcher.getFetchedValue();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            pendingFlagRemoved = false;
        }
        return pendingFlagRemoved;
    }
}
