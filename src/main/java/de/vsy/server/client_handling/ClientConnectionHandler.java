
package de.vsy.server.client_handling;

import de.vsy.server.client_handling.data_management.HandlerLocalDataManager;
import de.vsy.server.client_handling.strategy.LoggedOutClientHandlingStrategy;
import de.vsy.server.client_handling.strategy.PacketHandlingStrategy;
import de.vsy.server.client_handling.strategy.PendingClientPacketHandling;
import de.vsy.server.client_handling.strategy.RegularPacketHandlingStrategy;
import de.vsy.shared_module.packet_management.PacketBuffer;
import de.vsy.shared_module.packet_transmission.ConnectionThreadControl;
import de.vsy.shared_module.packet_transmission.cache.UnconfirmedPacketTransmissionCache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static de.vsy.server.client_management.ClientState.AUTHENTICATED;
import static de.vsy.shared_utility.standard_value.ThreadContextValues.*;

/**
 * Handles client connection as well as processing requests and transferring them into server
 * readable structures if needed .
 */
public class ClientConnectionHandler implements Runnable {

    private static final String THREAD_BASE_NAME = "ClientHandler_";
    private static final Logger LOGGER = LogManager.getLogger();
    private final Socket clientConnection;
    private final HandlerLocalDataManager threadDataManager;
    private ConnectionThreadControl connectionControl;

    /**
     * Instantiates a new client handler.
     *
     * @param clientSocket the client socket
     */
    public ClientConnectionHandler(final Socket clientSocket,
                                   final PacketBuffer requestAssignmentBuffer) {
        this.clientConnection = clientSocket;
        this.threadDataManager = new HandlerLocalDataManager(requestAssignmentBuffer);
    }

    @Override
    public void run() {
        PacketHandlingStrategy clientHandling;

        finishThreadSetup();

        if (this.connectionControl.initiateConnectionThreads()) {
            boolean threadInterrupted = false;
            clientHandling = new RegularPacketHandlingStrategy(this.threadDataManager,
                    this.connectionControl);

            while (this.connectionControl.connectionIsLive() && !(threadInterrupted)) {
                clientHandling.administerStrategy();
                threadInterrupted = Thread.interrupted();
            }
            this.connectionControl.closeConnection();
            LOGGER.info("Client connection terminated.");
            clientHandling = getPacketHandlingStrategy(threadInterrupted);

            if (clientHandling != null) {
                clientHandling.administerStrategy();
            }
        } else {
            LOGGER.info("Client connection failed.");
        }
        finishThreadTermination();
    }

    private PacketHandlingStrategy getPacketHandlingStrategy(final boolean threadWasInterrupted) {
        var stateManager = this.threadDataManager.getClientStateManager();
        final var stillAuthenticated = stateManager.checkClientState(AUTHENTICATED);

        if (stillAuthenticated && !(threadWasInterrupted)) {
            LOGGER.info("Client connection lost, therefore client will be handled as pending.");
            return new PendingClientPacketHandling(this.threadDataManager,
                    this.connectionControl);
        } else if (threadWasInterrupted || !(stateManager.isAccountDeleted())) {
            LOGGER.info("Client logged out: {}. Remaining Packets will be retained, if possible.", !threadWasInterrupted);
            return new LoggedOutClientHandlingStrategy(this.threadDataManager.getHandlerBufferManager());
        } else {
            LOGGER.info("Client account deleted, no data will be rescued.");
            stateManager.setAccountDeleted(false);
        }
        return null;
    }

    /**
     * Finish thread setup.
     */
    private void finishThreadSetup() {
        var localDate = LocalDateTime.now();
        var threadName = THREAD_BASE_NAME + localDate.format(DateTimeFormatter.ISO_LOCAL_DATE) + "_"
                + localDate.getNano();

        ThreadContext.clearAll();
        ThreadContext.put(LOG_ROUTE_CONTEXT_KEY, STANDARD_CLIENT_ROUTE_VALUE);
        ThreadContext.put(LOG_FILE_CONTEXT_KEY, threadName);
        Thread.currentThread().setName(threadName);

        this.connectionControl = new ConnectionThreadControl(this.clientConnection,
                this.threadDataManager.getHandlerBufferManager(),
                new UnconfirmedPacketTransmissionCache(1000), true);
    }

    /**
     * Finish thread termination.
     */
    private void finishThreadTermination() {
        ThreadContext.clearAll();
    }
}
