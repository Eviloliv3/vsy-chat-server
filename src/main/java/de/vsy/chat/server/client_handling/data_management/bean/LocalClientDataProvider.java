package de.vsy.chat.server.client_handling.data_management.bean;

import de.vsy.chat.server.persistent_data.data_bean.CommunicatorData;
import de.vsy.chat.shared_transmission.dto.CommunicatorDTO;

public
interface LocalClientDataProvider {

    int getClientId ();

    CommunicatorData getClientData ();

    CommunicatorDTO getCommunicatorData ();
}
