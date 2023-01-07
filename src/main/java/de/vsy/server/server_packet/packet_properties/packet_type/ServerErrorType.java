package de.vsy.server.server_packet.packet_properties.packet_type;

import com.fasterxml.jackson.annotation.JsonTypeName;
import de.vsy.shared_transmission.packet.property.packet_type.PacketType;

/**
 * The Enum ServerErrorType.
 */
@JsonTypeName("serverError")
public enum ServerErrorType implements PacketType {
    SERVER_STATUS
}
