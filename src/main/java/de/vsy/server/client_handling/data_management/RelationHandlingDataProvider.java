package de.vsy.server.client_handling.data_management;

import de.vsy.server.client_handling.data_management.logic.ClientStateControl;

/**
 * Provides RELATION related Packet handlers with appropriate data access.
 */
public interface RelationHandlingDataProvider extends BaseHandlingDataProvider {

    CommunicationEntityDataProvider getContactToActiveClientMapper();

    ClientStateControl getClientStateControl();

    LocalClientStateObserverManager getLocalClientStateObserverManager();
}
