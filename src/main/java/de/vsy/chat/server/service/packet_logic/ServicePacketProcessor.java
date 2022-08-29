/*
 *
 */
package de.vsy.chat.server.service.packet_logic;

import de.vsy.chat.shared_module.packet_exception.PacketProcessingException;
import de.vsy.chat.shared_transmission.packet.Packet;

/** The Interface ServicePacketProcessor. */
public
interface ServicePacketProcessor {

    /**
     * Process Packet
     *
     * @param input the input
     *
     * @return the processor Packetresponse map
     *
     * @throws PacketProcessingException the PacketHandling exception
     */
    PacketResponseMap processPacket (Packet input)
    throws PacketProcessingException;
}
