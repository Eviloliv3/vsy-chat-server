package de.vsy.server.client_handling.data_management.bean;

import de.vsy.server.persistent_data.data_bean.CommunicatorData;
import de.vsy.server.persistent_data.data_bean.ConvertCommDataToDTO;
import de.vsy.shared_module.packet_management.ClientDataProvider;
import de.vsy.shared_transmission.dto.CommunicatorDTO;

import static de.vsy.server.persistent_data.EmptyDataProvider.EMPTY_COMMUNICATOR_DATA;

public class ClientDataManager implements ClientDataProvider {

    private CommunicatorData clientData;
    private CommunicatorDTO communicatorData;

    public ClientDataManager() {
        setCommunicatorData(null);
    }

    @Override
    public int getClientId() {
        return this.clientData.getCommunicatorId();
    }

    @Override
    public CommunicatorDTO getCommunicatorData() {
        return this.communicatorData;
    }

    public final void setCommunicatorData(CommunicatorData newClientData) {
        this.clientData = newClientData != null ? newClientData : EMPTY_COMMUNICATOR_DATA;
        this.communicatorData = ConvertCommDataToDTO.convertFrom(clientData);
    }
}
