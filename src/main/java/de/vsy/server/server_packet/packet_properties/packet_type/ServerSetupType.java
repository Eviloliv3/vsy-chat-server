
package de.vsy.server.server_packet.packet_properties.packet_type;

import com.fasterxml.jackson.annotation.JsonTypeName;
import de.vsy.shared_transmission.packet.property.packet_type.PacketType;

@JsonTypeName("serverSetup")
public enum ServerSetupType implements PacketType {
    INTER_SERVER_SYNC
}
