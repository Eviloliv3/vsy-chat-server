
package de.vsy.server.service.packet_logic;

import de.vsy.shared_module.packet_processing.PacketProcessor;
import de.vsy.shared_transmission.packet.property.packet_type.PacketType;

public interface ServicePacketProcessorFactory {

    /**
     * Returns a PacketProcessor capable of processing Packets of the specified type.
     *
     * @param type the PacketType
     * @return the PacketProcessor, null if none was found
     */
    PacketProcessor getPacketProcessor(PacketType type);
}
