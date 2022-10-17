package de.vsy.server.server_packet.packet_creation;

import de.vsy.server.server_packet.content.BaseStatusSyncDTO;
import de.vsy.server.server_packet.content.ExtendedStatusSyncDTO;
import de.vsy.server.server_packet.content.InterServerCommSyncDTO;
import de.vsy.server.server_packet.packet_properties.packet_identifier.ServerUpdateIdentifier;
import de.vsy.server.server_packet.packet_properties.packet_type.ServerStatusType;
import de.vsy.shared_module.shared_module.packet_creation.identification_provider.AbstractIdentificationProvider;

public class ServerStatusIdentificationProvider extends AbstractIdentificationProvider {

  {
    identifiers.put(InterServerCommSyncDTO.class,
        () -> new ServerUpdateIdentifier(ServerStatusType.SERVER_STATUS));
    identifiers.put(BaseStatusSyncDTO.class,
        () -> new ServerUpdateIdentifier(ServerStatusType.CLIENT_STATUS));
    identifiers.put(ExtendedStatusSyncDTO.class,
        () -> new ServerUpdateIdentifier(ServerStatusType.CLIENT_STATUS));
  }
}
