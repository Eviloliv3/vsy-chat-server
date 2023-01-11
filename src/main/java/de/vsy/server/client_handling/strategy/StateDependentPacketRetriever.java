package de.vsy.server.client_handling.strategy;

import de.vsy.server.client_handling.data_management.bean.ClientStateListener;
import de.vsy.server.client_handling.packet_processing.request_filter.PermittedPacketCategoryCheck;
import de.vsy.server.client_handling.persistent_data_access.ClientPersistentDataAccessProvider;
import de.vsy.server.client_management.ClientState;
import de.vsy.server.persistent_data.client_data.PendingType;
import de.vsy.shared_module.packet_management.ThreadPacketBufferLabel;
import de.vsy.shared_module.packet_management.ThreadPacketBufferManager;
import de.vsy.shared_transmission.packet.Packet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class StateDependentPacketRetriever {

    private static final Logger LOGGER = LogManager.getLogger();
    private final ClientPersistentDataAccessProvider persistentDataAccess;
    private final ThreadPacketBufferManager packetBuffers;
    private final EnumMap<PendingType, Supplier<Consumer<Packet>>> strategyEnumMap;
    private final PermittedPacketCategoryCheck categoryCheck;

    {
        strategyEnumMap = new EnumMap<>(PendingType.class);
        strategyEnumMap.put(PendingType.CLIENT_BOUND, this::getClientBoundStrategy);
        strategyEnumMap.put(PendingType.PROCESSOR_BOUND, this::getProcessingPacketStrategy);
    }

    public StateDependentPacketRetriever(final ClientPersistentDataAccessProvider persistentData,
                                         final ThreadPacketBufferManager packetBuffers, final PermittedPacketCategoryCheck categoryCheck) {
        this.persistentDataAccess = persistentData;
        this.packetBuffers = packetBuffers;
        this.categoryCheck = categoryCheck;
    }

    public void getPacketsFor(final ClientState changedState) {

            final var pendingPacketProvider = this.persistentDataAccess.getPendingPacketDAO();
            final var allPendingPackets = pendingPacketProvider.readAllPendingPackets();
            final var remainingPackets = new LinkedHashMap<String, Packet>();

            for (final var currentPendingPacketMap : allPendingPackets.entrySet()) {
                final var currentPendingType = currentPendingPacketMap.getKey();
                final var sortedPendingPackets = currentPendingPacketMap.getValue();
                final var foundStrategy = strategyEnumMap.get(currentPendingType);

                if (foundStrategy != null) {
                    final var strategy = foundStrategy.get();

                    for(final var pendingPacket : sortedPendingPackets.values()){
                        if(this.categoryCheck.checkPacketCategory(pendingPacket.getPacketProperties().getPacketIdentificationProvider().getPacketCategory())){
                            strategy.accept(pendingPacket);
                        }else{
                            remainingPackets.put(pendingPacket.getPacketHash(), pendingPacket);
                        }
                    }
                    LOGGER.trace("Existing packets were prepended for PendingType: {}",
                            currentPendingType);
                } else {
                    LOGGER.warn("No strategy found for PendingType: {}", currentPendingType);
                }
                pendingPacketProvider.setPendingPackets(currentPendingType, remainingPackets);
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
