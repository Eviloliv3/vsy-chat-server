package de.vsy.server.data.access;

import de.vsy.server.service.ServicePacketBufferManager;

/**
 * The Interface ErrorHandlingServiceDataProvider.
 */
public interface ErrorHandlingServiceDataProvider extends ServiceBaseDataProvider {

    /**
     * Returns the service PacketBuffer manager.
     *
     * @return the service PacketBuffer manager
     */
    ServicePacketBufferManager getServicePacketBufferManager();
}
