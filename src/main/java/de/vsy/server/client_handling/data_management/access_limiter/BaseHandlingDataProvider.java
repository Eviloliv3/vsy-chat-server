package de.vsy.server.client_handling.data_management.access_limiter;

import de.vsy.server.server_packet.packet_creation.ResultingPacketContentHandler;
import de.vsy.shared_module.packet_management.ClientDataProvider;

public interface BaseHandlingDataProvider {

    ResultingPacketContentHandler getResultingPacketContentHandler();

    ClientDataProvider getLocalClientDataProvider();

}
