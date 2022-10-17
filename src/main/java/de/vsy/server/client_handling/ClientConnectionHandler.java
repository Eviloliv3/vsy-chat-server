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
import de.vsy.server.client_handling.strategy.PendingChatPacketHandling;
import de.vsy.server.client_handling.strategy.RegularPacketHandlingStrategy;
import de.vsy.server.persistent_data.client_data.PendingType;
import de.vsy.server.server.client_management.ClientState;
import de.vsy.shared_module.shared_module.packet_management.PacketBuffer;
import de.vsy.shared_module.shared_module.packet_management.ThreadPacketBufferLabel;
import de.vsy.shared_module.shared_module.packet_transmission.ConnectionThreadControl;
import de.vsy.shared_module.shared_module.packet_transmission.cache.UnconfirmedPacketTransmissionCache;
import de.vsy.shared_transmission.shared_transmission.packet.Packet;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Queue;
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

      while (this.connectionControl.connectionIsLive() && !Thread.currentThread().isInterrupted()) {
        clientHandling.administerStrategy();
      }

      if (stateManager.checkClientState(ClientState.AUTHENTICATED)) {
        LOGGER.info("Klient hat sich nicht abgemeldet und wird in schwebenden Zustand versetzt.");
        saveClientBoundPackets();
        clientHandling = new PendingChatPacketHandling(threadDataManager);
        clientHandling.administerStrategy();
      } else {
        clearAllBuffers();
      }
      this.connectionControl.closeConnection();
      LOGGER.info("Verbindung zum Klienten beendet.");
    } else {
      LOGGER.info("Verbindung zum Klienten fehlgeschlagen.");
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
   * Removes Packets from UnconfirmedPacketTransmissionCache and then CLIENT_BOUND buffer. Saves
   * them in with PendingPacketDAO.
   */
  private void saveClientBoundPackets() {
    var reinterrupt = false;
    final var pendingPacketAccessor = this.threadDataManager.getLocalClientStateDependentLogicProvider()
        .getClientPersistentAccess().getPendingPacketDAO();

    if (pendingPacketAccessor != null) {
      final var handlerBuffer = this.threadDataManager.getHandlerBufferManager()
          .getPacketBuffer(ThreadPacketBufferLabel.OUTSIDE_BOUND);
      UnconfirmedPacketTransmissionCache cache = this.connectionControl.getPacketCache();
      final Queue<Packet> notReceivedPackets = cache.removeRemainingPackets();

      while (!notReceivedPackets.isEmpty()) {
        pendingPacketAccessor.appendPendingPacket(PendingType.CLIENT_BOUND,
            notReceivedPackets.poll());
      }

      while (handlerBuffer.containsPackets()) {
        try {
          final var currentPacket = handlerBuffer.getPacket();
          pendingPacketAccessor.appendPendingPacket(PendingType.CLIENT_BOUND, currentPacket);
        } catch (InterruptedException e) {
          reinterrupt = true;
          LOGGER.error("Buffer wird geleert. Interrupt wird " + "zeitweise ignoriert.");
        }
      }

      if (reinterrupt) {
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * Try buffer clearing.
   */
  private void clearAllBuffers() {
    // TODO Klient hat sich abgemeldet. Also müssen noch die verbleibenden Pakete
    // sinnvoll weiterverarbeitet oder zurückgesandt werden.
    // nicht: ist kein Paket zu verarbeiten
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
