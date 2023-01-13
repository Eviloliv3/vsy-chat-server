package de.vsy.server.client_handling.data_management;

import de.vsy.server.server_packet.packet_creation.ResultingPacketContentHandler;
import de.vsy.shared_module.packet_management.ClientDataProvider;

/**
 * Provides data access appropriate for all PacketCategory handlers.
 */
public interface BaseHandlingDataProvider {

    ResultingPacketContentHandler getResultingPacketContentHandler();

    ClientDataProvider getLocalClientDataProvider();

}
