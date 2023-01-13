package de.vsy.server.data.access;

import de.vsy.server.service.ServicePacketBufferManager;

/**
 * Provides appropriate server data access for Notification synchronization services.
 */
public interface ErrorHandlingServiceDataProvider extends ServiceBaseDataProvider {

    ServicePacketBufferManager getServicePacketBufferManager();
}
