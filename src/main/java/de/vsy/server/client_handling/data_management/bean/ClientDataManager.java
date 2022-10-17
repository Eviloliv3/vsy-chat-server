package de.vsy.server.client_handling.data_management.bean;

import static de.vsy.server.persistent_data.EmptyDataProvider.EMPTY_COMMUNICATOR_DATA;

import de.vsy.server.persistent_data.data_bean.CommunicatorData;
import de.vsy.server.persistent_data.data_bean.ConvertCommDataToDTO;
import de.vsy.shared_transmission.shared_transmission.dto.CommunicatorDTO;

public class ClientDataManager implements LocalClientDataProvider {

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
	public CommunicatorData getClientData() {
		return this.clientData;
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
