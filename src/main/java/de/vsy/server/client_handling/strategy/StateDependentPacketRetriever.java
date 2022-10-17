package de.vsy.server.client_handling.strategy;

import de.vsy.server.client_handling.data_management.bean.ClientStateListener;
import de.vsy.server.client_handling.persistent_data_access.ClientPersistentDataAccessProvider;
import de.vsy.server.persistent_data.client_data.PendingType;
import de.vsy.server.server.client_management.ClientState;
import de.vsy.shared_module.shared_module.packet_management.ThreadPacketBufferLabel;
import de.vsy.shared_module.shared_module.packet_management.ThreadPacketBufferManager;
import de.vsy.shared_transmission.shared_transmission.packet.Packet;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StateDependentPacketRetriever implements ClientStateListener {

  private static final Logger LOGGER = LogManager.getLogger();
  private final ClientPersistentDataAccessProvider persistentDataAccess;
  private final ThreadPacketBufferManager packetBuffers;
  private final EnumMap<PendingType, Supplier<Consumer<Packet>>> strategyEnumMap;

  {
    strategyEnumMap = new EnumMap<>(PendingType.class);
    strategyEnumMap.put(PendingType.CLIENT_BOUND, this::getClientBoundStrategy);
    strategyEnumMap.put(PendingType.PROCESSOR_BOUND, this::getProcessingPacketStrategy);
  }

  public StateDependentPacketRetriever(final ClientPersistentDataAccessProvider persistentData,
      final ThreadPacketBufferManager packetBuffers) {
    this.persistentDataAccess = persistentData;
    this.packetBuffers = packetBuffers;
  }

  @Override
  public void evaluateNewState(ClientState changedState, boolean added) {

    if (added) {
      final var pendingPacketProvider = this.persistentDataAccess.getPendingPacketDAO();
      final var allPendingPackets = pendingPacketProvider.readAllPendingPackets();
      final var remainingPackets = new LinkedHashMap<String, Packet>();

      for (final var currentPendingPacketMap : allPendingPackets.entrySet()) {
        final var currentClassification = currentPendingPacketMap.getKey();
        final var pendingMap = currentPendingPacketMap.getValue();
        var foundStrategy = strategyEnumMap.computeIfPresent(currentClassification,
            (classification, strategySupplier) -> {
              final var strategy = strategySupplier.get();
              pendingMap.values().forEach(strategy);
              return strategySupplier;
            });

        if (foundStrategy != null) {
          LOGGER.trace("Vorhandene Pakete wurden Buffern vorangestellt. PendingType: {}",
              currentClassification);
        } else {
          LOGGER.warn("Es wurde keine Strategie gefunden. PendingType: {}", currentClassification);
        }

        pendingPacketProvider.setPendingPackets(currentClassification, remainingPackets);
      }
    }
  }

  private Consumer<Packet> getClientBoundStrategy() {
    final var clientBuffer = this.packetBuffers.getPacketBuffer(
        ThreadPacketBufferLabel.OUTSIDE_BOUND);
    return clientBuffer::appendPacket;
  }

  private Consumer<Packet> getProcessingPacketStrategy() {
    final var clientBuffer = this.packetBuffers.getPacketBuffer(
        ThreadPacketBufferLabel.HANDLER_BOUND);
    return clientBuffer::prependPacket;
  }
}
