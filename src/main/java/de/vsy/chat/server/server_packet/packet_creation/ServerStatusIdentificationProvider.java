package de.vsy.chat.server.server_packet.packet_creation;

import de.vsy.chat.shared_module.packet_creation.identification_provider.AbstractIdentificationProvider;
import de.vsy.chat.server.server_packet.content.ExtendedStatusSyncDTO;
import de.vsy.chat.server.server_packet.content.InterServerCommSyncDTO;
import de.vsy.chat.server.server_packet.content.ReconnectNotificationDTO;
import de.vsy.chat.server.server_packet.content.SimpleStatusSyncDTO;
import de.vsy.chat.server.server_packet.packet_properties.packet_identifier.ServerUpdateIdentifier;
import de.vsy.chat.server.server_packet.packet_properties.packet_type.ServerStatusType;

public
class ServerStatusIdentificationProvider extends AbstractIdentificationProvider {

    {
        identifiers.put(ReconnectNotificationDTO.class,
                        () -> new ServerUpdateIdentifier(
                                ServerStatusType.CLIENT_STATUS));
        identifiers.put(InterServerCommSyncDTO.class,
                        () -> new ServerUpdateIdentifier(
                                ServerStatusType.SERVER_STATUS));
        identifiers.put(SimpleStatusSyncDTO.class, () -> new ServerUpdateIdentifier(
                ServerStatusType.CLIENT_STATUS));
        identifiers.put(ExtendedStatusSyncDTO.class,
                        () -> new ServerUpdateIdentifier(
                                ServerStatusType.CLIENT_STATUS));
    }
}
