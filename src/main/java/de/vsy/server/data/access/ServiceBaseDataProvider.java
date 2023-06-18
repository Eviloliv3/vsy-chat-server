package de.vsy.server.data.access;

import de.vsy.server.data.PacketCategorySubscriptionManager;
import de.vsy.server.data.ServerSynchronizationManager;
import de.vsy.server.data.socketConnection.LocalServerConnectionData;

/**
 * Provides appropriate server data access for all services.
 */
public interface ServiceBaseDataProvider {

    /**
     * CLIENT side of publish-subscribe network.
     *
     * @return PacketCategorySubscriptionManager used for client subscriptions.
     */
    PacketCategorySubscriptionManager getClientSubscriptionManager();

    /**
     * SERVER side of publish-subscribe network.
     *
     * @return PacketCategorySubscriptionManager used for service subscriptions.
     */
    PacketCategorySubscriptionManager getServiceSubscriptionManager();

    LocalServerConnectionData getLocalServerConnectionData();

    ServerSynchronizationManager getServerSynchronizationManager();
}
