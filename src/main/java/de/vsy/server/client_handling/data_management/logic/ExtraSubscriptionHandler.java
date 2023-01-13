package de.vsy.server.client_handling.data_management.logic;

import de.vsy.shared_transmission.packet.property.packet_category.PacketCategory;

@FunctionalInterface
public interface ExtraSubscriptionHandler {
    /**
     * Handles the persistent saving of the specified client's additional subscription.
     *
     * @param clientId the client's id
     * @param topic    the additional subscriptions topic
     * @param threadId the additional subscriptions thread id
     * @return true if the additional subscription was saved, false otherwise
     */
    boolean handle(final int clientId, final PacketCategory topic, final int threadId);
}
