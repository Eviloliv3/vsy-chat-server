package de.vsy.server.client_handling.data_management.access_limiter;

import de.vsy.server.client_handling.data_management.bean.LocalClientDataProvider;
import de.vsy.server.client_handling.data_management.bean.LocalClientStateProvider;
import de.vsy.server.server_packet.packet_creation.ResultingPacketContentHandler;

public interface BaseHandlingDataProvider {

  ResultingPacketContentHandler getResultingPacketContentHandler();

  LocalClientDataProvider getLocalClientDataProvider();

  LocalClientStateProvider getLocalClientStateProvider();
}
