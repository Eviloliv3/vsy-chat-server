/*
 *
 */
package de.vsy.server.client_handling;

import static de.vsy.shared_utility.standard_value.ThreadContextValues.LOG_FILE_CONTEXT_KEY;
import static de.vsy.shared_utility.standard_value.ThreadContextValues.LOG_ROUTE_CONTEXT_KEY;
import static de.vsy.shared_utility.standard_value.ThreadContextValues.STANDARD_CLIENT_ROUTE_VALUE;

import de.vsy.server.client_handling.data_management.HandlerLocalDataManager;
import de.vsy.server.client_handling.data_management.bean.ClientStateManager;
import de.vsy.server.client_handling.strategy.PacketHandlingStrategy;
import de.vsy.server.client_handling.strategy.PendingClientPacketHandling;
import de.vsy.server.client_handling.strategy.RegularPacketHandlingStrategy;
import de.vsy.server.client_management.ClientState;
import de.vsy.shared_module.packet_management.PacketBuffer;
import de.vsy.shared_module.packet_transmission.ConnectionThreadControl;
import de.vsy.shared_module.packet_transmission.cache.UnconfirmedPacketTransmissionCache;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

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
    ClientStateManager stateManager;
    PacketHandlingStrategy clientHandling;

    finishThreadSetup();
    stateManager = this.threadDataManager.getClientStateManager();

    if (this.connectionControl.initiateConnectionThreads()) {
      clientHandling = new RegularPacketHandlingStrategy(this.threadDataManager,
          this.connectionControl);

      while (this.connectionControl.connectionIsLive() && !Thread.interrupted()) {
        clientHandling.administerStrategy();
      }

      if (stateManager.checkClientState(ClientState.AUTHENTICATED)) {
        LOGGER.info("Client not logged out, therefore client will be handled as pending.");
        clientHandling = new PendingClientPacketHandling(this.threadDataManager,
            this.connectionControl);
        clientHandling.administerStrategy();
      } else {
        LOGGER.error("!! Buffers not cleared.");
        //clearAllBuffers();
      }
      this.connectionControl.closeConnection();
      LOGGER.info("Client connection terminated.");
    } else {
      LOGGER.info("Client connection failed.");
    }
    finishThreadTermination();
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
   * Try buffer clearing.
   */
  private void clearAllBuffers() {
    // TODO client logged out, remaining packets should be processed in a sensible way, or sent back.
    // clearClientBoundBuffer();
    // clearHandlerBoundBuffer();
  }

  /**
   * Finish thread termination.
   */
  private void finishThreadTermination() {
    ThreadContext.clearAll();
  }
}
