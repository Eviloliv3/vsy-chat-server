/*
 *
 */
package de.vsy.chat.server.service.packet_logic;

import de.vsy.chat.shared_transmission.packet.property.packet_type.PacketType;

public
interface ServicePacketProcessorFactory {

    /**
     * Gets the PacketHandler.
     *
     * @param type the type
     *
     * @return the PacketHandler
     */
    ServicePacketProcessor getPacketProcessor (PacketType type);
}
