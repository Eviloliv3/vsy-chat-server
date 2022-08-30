package de.vsy.server.server.data.access;

import de.vsy.server.server.data.AbstractPacketCategorySubscriptionManager;

public
interface ServiceBaseDataProvider {
    /**
     * Gets the thread status manipulator.
     *
     * @return the thread status manipulator
     */
    // ThreadStatusManipulator getThreadStatusManipulator ();

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
}
