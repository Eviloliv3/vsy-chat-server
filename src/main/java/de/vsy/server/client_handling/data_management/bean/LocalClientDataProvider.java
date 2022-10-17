package de.vsy.server.client_handling.data_management.bean;

import de.vsy.server.persistent_data.data_bean.CommunicatorData;
import de.vsy.shared_transmission.shared_transmission.dto.CommunicatorDTO;

public interface LocalClientDataProvider {

  int getClientId();

  CommunicatorData getClientData();

  CommunicatorDTO getCommunicatorData();
}
