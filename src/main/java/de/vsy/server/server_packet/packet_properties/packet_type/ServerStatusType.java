package de.vsy.server.server_packet.packet_properties.packet_type;

import com.fasterxml.jackson.annotation.JsonTypeName;
import de.vsy.shared_transmission.packet.property.packet_type.PacketType;

/**
 * The Enum ServerUpdateType.
 */
@JsonTypeName("serverStatus")
public enum ServerStatusType implements PacketType {
    SERVER_STATUS, CLIENT_STATUS
}
