package de.vsy.chat.server.client_handling.strategy;

import de.vsy.chat.shared_transmission.packet.Packet;

/** The Interface PendingPacketHandlingStrategy. */
public
interface PendingPacketHandlingStrategy {

    /**
     * Handle Packet
     *
     * @param input the input
     *
     * @return true, if successful
     */
    boolean handlePacket (Packet input);
}
