package de.vsy.server.persistent_data;

import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_CLIENT_ID;
import static de.vsy.shared_utility.standard_value.StandardStringProvider.STANDARD_EMPTY_STRING;

import de.vsy.server.persistent_data.data_bean.CommunicatorData;
import de.vsy.shared_transmission.shared_transmission.dto.standard_empty_value.StandardEmptyDataProvider;

public class EmptyDataProvider extends StandardEmptyDataProvider {

  public static final CommunicatorData EMPTY_COMMUNICATOR_DATA = CommunicatorData.valueOf(
      STANDARD_CLIENT_ID,
      STANDARD_CLIENT_ID, STANDARD_EMPTY_STRING);

  private EmptyDataProvider() {
  }
}
