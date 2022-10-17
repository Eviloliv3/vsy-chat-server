package de.vsy.server.client_handling.data_management.access_limiter;

import de.vsy.server.client_handling.data_management.LocalClientStateDependentLogicProvider;

/**
 * Provides chat PacketCategory handlers with dataManagement accessLimiter.
 */
public interface ChatHandlingDataProvider extends BaseHandlingDataProvider {

  LocalClientStateDependentLogicProvider getLocalClientStateDependentLogicProvider();
}
