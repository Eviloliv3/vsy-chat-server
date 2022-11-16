package de.vsy.server.client_handling.strategy;

import static java.util.Arrays.asList;

import de.vsy.server.client_handling.data_management.HandlerLocalDataManager;
import de.vsy.server.client_handling.data_management.bean.LocalClientDataProvider;
import de.vsy.server.client_handling.data_management.logic.AuthenticationStateControl;
import de.vsy.server.client_management.ClientState;
import de.vsy.server.persistent_data.client_data.PendingPacketDAO;
import de.vsy.server.persistent_data.client_data.PendingType;
import de.vsy.shared_module.shared_module.packet_management.PacketBuffer;
import de.vsy.shared_module.shared_module.packet_management.ThreadPacketBufferLabel;
import de.vsy.shared_transmission.shared_transmission.packet.Packet;
import de.vsy.shared_utility.async_value_acquisition.TimeBasedValueFetcher;
import de.vsy.shared_utility.logging.ThreadContextRunnable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The Class PendingClientBufferWatcher.
 */
public class PendingClientBufferWatcher extends ThreadContextRunnable {

  private static final long PENDING_END = 25000L;
  private static final int TERMINATION_LATCH_COUNT = 1;
  private static final Logger LOGGER = LogManager.getLogger();
  private final PacketBuffer clientBuffer;
  private final CountDownLatch terminationLatch;
  private final PendingPacketDAO pendingPacketAccessor;
  private final AuthenticationStateControl clientStateControl;
  private final LocalClientDataProvider localClientData;

  /**
   * Instantiates a new pending client buffer watcher.
   *
   * @param threadDataAccess the thread data access
   */
  public PendingClientBufferWatcher(final HandlerLocalDataManager threadDataAccess) {
    super();
    this.pendingPacketAccessor = threadDataAccess.getLocalClientStateDependentLogicProvider()
        .getClientPersistentAccess().getPendingPacketDAO();
    this.localClientData = threadDataAccess.getLocalClientDataProvider();
    this.clientBuffer = threadDataAccess.getHandlerBufferManager()
        .getPacketBuffer(ThreadPacketBufferLabel.HANDLER_BOUND);
    this.clientStateControl = threadDataAccess.getGlobalAuthenticationStateControl();
    this.terminationLatch = new CountDownLatch(TERMINATION_LATCH_COUNT);
  }

  @Override
  protected void runWithContext() {
    LOGGER.info("{}-PendingClientBufferWatcher initiated.", this.localClientData.getClientId());
    TimeBasedValueFetcher<Boolean> reconnectFlagFetcher;
    ScheduledExecutorService reconnectFlagCheck = Executors.newSingleThreadScheduledExecutor();
    Instant terminationTime = Instant.now().plusMillis(PENDING_END);

    reconnectFlagFetcher = new TimeBasedValueFetcher<>(
        this.clientStateControl::getReconnectionState, true,
        terminationTime, terminationLatch);
    reconnectFlagCheck.scheduleWithFixedDelay(reconnectFlagFetcher, 20, 50, TimeUnit.MILLISECONDS);
    saveIncomingPackets();
    reconnectFlagCheck.shutdownNow();

    evaluateReconnectionState(reconnectFlagFetcher);
    LOGGER.info("PendingClientBufferWatcher ended.");
  }

  /**
   * Save incoming Packet.
   */
  private void saveIncomingPackets() {
    var reInterrupt = false;
    try {
      this.pendingPacketAccessor.createFileAccess(this.localClientData.getClientId());
    } catch (InterruptedException ie) {
      throw new IllegalStateException(
          "Dateizugriff fehlgeschlagen.\n" + ie.getMessage() + "\n" + asList(ie.getStackTrace()));
    }

    while (this.terminationLatch.getCount() == TERMINATION_LATCH_COUNT) {
      try {
        Packet currentPacket = clientBuffer.getPacket();
        if (currentPacket != null) {
          this.pendingPacketAccessor.appendPendingPacket(PendingType.PROCESSOR_BOUND,
              currentPacket);
        }
      } catch (InterruptedException ie) {
        reInterrupt = true;
        LOGGER.warn("Watcher will continue working, because all packets have to be saved "
            + "persistently. Thread will end after specified time.");
      }
    }

    if (reInterrupt) {
      Thread.currentThread().interrupt();
    }
  }

  private void evaluateReconnectionState(TimeBasedValueFetcher<Boolean> reconnectFlagFetcher) {
    final boolean hasReconnected = reconnectFlagFetcher.getFetchedValue();

    if (hasReconnected) {
      LOGGER.info("Client reconnected.");
      this.clientStateControl.changePendingState(false);
    } else {
      LOGGER.info("Client timed out, will now be logged out.");
      this.removeVolatilePendingPackets();
      this.clientStateControl.changePersistentClientState(ClientState.AUTHENTICATED, false);
      this.clientStateControl.logoutClient();
    }
  }

  private void removeVolatilePendingPackets() {
    for (final var pendingDirection : PendingType.values()) {
      var allPackets = this.pendingPacketAccessor.readPendingPackets(pendingDirection);
      var nonVolatilePackets = removePackets(allPackets);
      this.pendingPacketAccessor.setPendingPackets(pendingDirection, nonVolatilePackets);
    }
  }

  private Map<String, Packet> removePackets(Map<String, Packet> allPackets) {
    Map<String, Packet> nonVolatilePackets = new HashMap<>();

    for (var currentPacketSet : allPackets.entrySet()) {
      final var currentPacket = currentPacketSet.getValue();

      if (!VolatilePacketIdentifier.checkPacketVolatiliy(currentPacket)) {
        nonVolatilePackets.put(currentPacketSet.getKey(), currentPacket);
      }
    }
    return nonVolatilePackets;
  }
}
