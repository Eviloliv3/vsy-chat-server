package de.vsy.server.client_handling.data_management;

/**
 * Provides CHAT related Packet handlers with appropriate data access.
 */
public interface ChatHandlingDataProvider extends BaseHandlingDataProvider {

    LocalClientStateObserverManager getLocalClientStateObserverManager();
}
