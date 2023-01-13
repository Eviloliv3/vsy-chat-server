package de.vsy.server.client_handling.data_management.logic;

import de.vsy.shared_module.packet_management.PacketBuffer;
import de.vsy.shared_transmission.packet.property.packet_category.PacketCategory;

@FunctionalInterface
public interface SubscriptionHandler {
    /**
     * Handles the persistent saving of the specified client's additional subscription.
     *
     * @param topic              the topic
     * @param threadId           the thread id
     * @param subscriptionBuffer the client's PacketBuffer
     * @return true if topic/thread combination was subscribed
     * to successfully, false otherwise
     */
    boolean handle(final PacketCategory topic, final int threadId,
                   final PacketBuffer subscriptionBuffer);
}
