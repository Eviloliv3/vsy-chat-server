package de.vsy.server.client_handling.data_management.access_limiter;

import de.vsy.server.client_handling.data_management.CommunicationEntityDataProvider;
import de.vsy.server.client_handling.data_management.LocalClientStateObserverManager;
import de.vsy.server.client_handling.data_management.logic.ClientStateControl;

/**
 * The Interface UpdateHandlingDataProvider.
 */
public interface StatusHandlingDataProvider extends BaseHandlingDataProvider {

  ClientStateControl getGlobalClientStateControl();

  CommunicationEntityDataProvider getContactToActiveClientMapper();

  LocalClientStateObserverManager getLocalClientStateDependentLogicProvider();
}
