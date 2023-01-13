package de.vsy.server.client_handling.strategy;

import de.vsy.server.client_handling.data_management.HandlerLocalDataManager;
import de.vsy.server.data.access.HandlerAccessManager;
import de.vsy.server.persistent_data.client_data.PendingType;
import de.vsy.shared_module.packet_management.ThreadPacketBufferLabel;
import de.vsy.shared_module.packet_transmission.ConnectionThreadControl;
import de.vsy.shared_module.packet_transmission.cache.UnconfirmedPacketTransmissionCache;
import de.vsy.shared_transmission.packet.Packet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Queue;

/**
 * The Class PendingClientPacketHandling.
 */
public class PendingClientPacketHandling implements PacketHandlingStrategy {

    private static final Logger LOGGER = LogManager.getLogger();
    private final HandlerLocalDataManager handlerDataManager;
    private final ConnectionThreadControl connectionControl;

    /**
     * Instantiates a new pending chat PacketHandling.
     *
     * @param threadDataAccess the thread dataManagement accessLimiter
     */
    public PendingClientPacketHandling(final HandlerLocalDataManager threadDataAccess,
                                       final ConnectionThreadControl connectionControl) {
        this.handlerDataManager = threadDataAccess;
        this.connectionControl = connectionControl;
    }

    @Override
    public void administerStrategy() {
        LOGGER.info("PendingClientHandler initiation started.");
        saveClientBoundPackets();

        if (this.handlerDataManager.getAuthenticationStateControl().changePersistentPendingState(true)) {
            HandlerAccessManager.getPendingClientWatcherManager().addPendingClient(this.handlerDataManager);
        } else {
            LOGGER.error("Global pending state could not be set for: {}",
                    this.handlerDataManager.getLocalClientDataProvider().getCommunicatorData());
        }
    }

    private void saveClientBoundPackets() {
        var reinterrupt = false;
        final var pendingPacketAccessor = this.handlerDataManager.getLocalClientStateObserverManager()
                .getClientPersistentAccess().getPendingPacketDAO();

        if (pendingPacketAccessor != null) {
            final var handlerBuffer = this.handlerDataManager.getHandlerBufferManager()
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
                    LOGGER.error("PacketBuffer has to be emptied be emptied. Interrupt suspended.");
                }
            }

            if (reinterrupt) {
                Thread.currentThread().interrupt();
            }
        } else {
            LOGGER.error("Kein");
        }
    }
}
