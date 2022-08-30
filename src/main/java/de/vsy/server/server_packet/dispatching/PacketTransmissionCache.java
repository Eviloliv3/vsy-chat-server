package de.vsy.server.server_packet.dispatching;

import de.vsy.shared_transmission.shared_transmission.packet.Packet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayDeque;
import java.util.Deque;

public
class PacketTransmissionCache {

    private final static Logger LOGGER = LogManager.getLogger();
    private final Deque<Packet> transmissionCache;

    public
    PacketTransmissionCache () {
        this.transmissionCache = new ArrayDeque<>(2);
    }

    /**
     * Adds the committed packet, if it is not null.
     *
     * @param toTransmit the packet to transmit
     */
    public
    void addPacket (Packet toTransmit) {
        if (toTransmit != null && !this.transmissionCache.contains(toTransmit)) {
            this.transmissionCache.push(toTransmit);
        } else {
            LOGGER.error(
                    "Paket wurde nicht hinzugefuegt. Keine null-Werte erlaubt.");
        }
    }

    /**
     * Discards all cached Packets and adds the error packet instead.
     *
     * @param error the error packet to transmit
     */
    public
    void putError (Packet error) {
        if (error != null) {
            this.transmissionCache.clear();
            this.transmissionCache.push(error);
        } else {
            LOGGER.error("Vorhandene Paket wurden nicht entfernt. Fehlerpaket " +
                         "wurde nicht hinzugefuegt. Keine null-Werte erlaubt.");
        }
    }

    /**
     * Uses a dispatcher object to transmit all cached packets.
     *
     * @param dispatcher the object used for transmission
     *
     * @throws IllegalArgumentException if dispatcher argument is null
     */
    public
    void transmitPackets (PacketDispatcher dispatcher) {
        if (dispatcher == null) {
            throw new IllegalArgumentException("Kein Dispatcher uebergeben.");
        }

        if (!this.transmissionCache.isEmpty()) {
            for (final var currentPacket : this.transmissionCache) {
                dispatcher.dispatchPacket(currentPacket);
            }
            this.transmissionCache.clear();
        } else {
            LOGGER.info("Keine Pakete zu versenden. Cache ist leer.");
        }
    }
}
