package de.vsy.chat.server.persistent_data;

import de.vsy.chat.server.persistent_data.data_bean.CommunicatorData;
import de.vsy.chat.shared_transmission.dto.standard_empty_value.StandardEmptyDataProvider;

import static de.vsy.chat.shared_utility.standard_value.StandardIdProvider.STANDARD_CLIENT_ID;
import static de.vsy.chat.shared_utility.standard_value.StandardStringProvider.STANDARD_EMPTY_STRING;

public
class EmptyDataProvider extends StandardEmptyDataProvider {

    public static final CommunicatorData EMPTY_COMMUNICATOR_DATA = CommunicatorData.valueOf(
            STANDARD_CLIENT_ID, STANDARD_CLIENT_ID, STANDARD_EMPTY_STRING);

    private
    EmptyDataProvider () {}
}
