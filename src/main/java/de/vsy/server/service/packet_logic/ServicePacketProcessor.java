/*
 *
 */
package de.vsy.server.service.packet_logic;

import de.vsy.shared_module.shared_module.packet_exception.PacketProcessingException;
import de.vsy.shared_transmission.shared_transmission.packet.Packet;

/** The Interface ServicePacketProcessor. */
public
interface ServicePacketProcessor {

    /**
     * Process Packet
     *
     * @param input the input
     *
     * @throws PacketProcessingException the PacketHandling exception
     */
    void processPacket (Packet input)
    throws PacketProcessingException;
}
