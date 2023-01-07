/*
 *
 */
package de.vsy.server.service.packet_logic;

import de.vsy.shared_transmission.packet.property.packet_type.PacketType;

public interface ServicePacketProcessorFactory {

    /**
     * Returns the PacketHandler.
     *
     * @param type the type
     * @return the PacketHandler
     */
    ServicePacketProcessor getPacketProcessor(PacketType type);
}
