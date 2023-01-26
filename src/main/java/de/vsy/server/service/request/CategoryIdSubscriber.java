package de.vsy.server.service.request;

import de.vsy.shared_module.packet_management.PacketBuffer;
import de.vsy.shared_transmission.packet.Packet;
import org.apache.logging.log4j.LogManager;

import java.util.HashSet;
import java.util.Set;

/**
 * The Class CategoryIdSubscriber.
 */
public class CategoryIdSubscriber {

    private final Set<PacketBuffer> subscriptionBuffers;

    /**
     * Instantiates a new category id subscriber.
     */
    public CategoryIdSubscriber() {this.subscriptionBuffers = new HashSet<>();}

    /**
     * Adds the subscription.
     *
     * @param newSubscription the new subscription
     * @return true, if successful
     */
    public boolean addSubscription(final PacketBuffer newSubscription) {

        if (newSubscription != null) {
            return this.subscriptionBuffers.add(newSubscription);
        }
        return false;
    }

    /**
     * Checks for subscribers.
     *
     * @return true, if successful
     */
    public boolean hasSubscribers() {
        return !this.subscriptionBuffers.isEmpty();
    }

    /**
     * Publish.
     *
     * @param toPublish the to publish
     */
    public void publish(final Packet toPublish) {
        if (toPublish == null) {
            throw new IllegalArgumentException("No Packet specified.");
        }

        for (var subscriptionBuffer : this.subscriptionBuffers) {
            subscriptionBuffer.appendPacket(toPublish);
        }
    }

    /**
     * Removes the subscription.
     *
     * @param subscriptionToRemove the subscription to remove
     * @return true, if successful
     */
    public boolean removeSubscription(final PacketBuffer subscriptionToRemove) {

        if (subscriptionToRemove != null) {
            return this.subscriptionBuffers.remove(subscriptionToRemove);
        }
        return false;
    }
}
