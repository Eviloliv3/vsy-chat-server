/*
 *
 */
package de.vsy.chat.server.server_packet.packet_properties.packet_type;

import com.fasterxml.jackson.annotation.JsonTypeName;
import de.vsy.chat.shared_transmission.packet.property.packet_type.PacketType;

@JsonTypeName("serverSetup")
public
enum ServerSetupType implements PacketType {
    INTERSERVER_SYNC
}
