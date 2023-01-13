package de.vsy.server.client_handling.data_management;

import de.vsy.server.client_handling.data_management.logic.ClientStateControl;

/**
 * Provides STATUS related Packet handlers with appropriate data access.
 */
public interface StatusHandlingDataProvider extends BaseHandlingDataProvider {

    ClientStateControl getClientStateControl();

    CommunicationEntityDataProvider getContactToActiveClientMapper();

    LocalClientStateObserverManager getLocalClientStateObserverManager();
}
