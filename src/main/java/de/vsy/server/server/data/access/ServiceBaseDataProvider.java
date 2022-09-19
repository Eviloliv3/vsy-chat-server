package de.vsy.server.server.data.access;

import de.vsy.server.server.data.AbstractPacketCategorySubscriptionManager;
import de.vsy.server.server.server_connection.LocalServerConnectionData;

public
interface ServiceBaseDataProvider {

    /**
     * Gets the client subscription manager.
     *
     * @return the client subscription manager
     */
    AbstractPacketCategorySubscriptionManager getClientSubscriptionManager ();

    /**
     * Gets the server subscription manager.
     *
     * @return the server subscription manager
     */
    AbstractPacketCategorySubscriptionManager getServiceSubscriptionManager ();

    LocalServerConnectionData getLocalServerConnectionData ();
}
