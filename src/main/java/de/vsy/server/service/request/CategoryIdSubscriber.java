package de.vsy.server.service.request;

import de.vsy.shared_module.shared_module.packet_management.PacketBuffer;
import de.vsy.shared_transmission.shared_transmission.packet.Packet;

import java.util.HashSet;
import java.util.Set;

/** The Class CategoryIdSubscriber. */
public
class CategoryIdSubscriber {

    private final Set<PacketBuffer> subscriptionBuffers;

    /** Instantiates a new category id subscriber. */
    public
    CategoryIdSubscriber () {
        this(new HashSet<>());
    }

    /**
     * Instantiates a new category id subscriber.
     *
     * @param categoryIdSubscribers the category id subscribers
     */
    public
    CategoryIdSubscriber (final Set<PacketBuffer> categoryIdSubscribers) {

        if (categoryIdSubscribers != null) {
            this.subscriptionBuffers = categoryIdSubscribers;
        } else {
            throw new IllegalArgumentException(
                    "null statt Set von PacketBuffern übergeben.");
        }
    }

    /**
     * Instantiates a new category id subscriber.
     *
     * @param subscriptionBuffer the subscription buffer
     */
    public
    CategoryIdSubscriber (final PacketBuffer subscriptionBuffer) {
        this.subscriptionBuffers = new HashSet<>();

        if (subscriptionBuffer != null) {
            this.subscriptionBuffers.add(subscriptionBuffer);
        } else {
            throw new IllegalArgumentException("null statt PacketBuffer übergeben.");
        }
    }

    /**
     * Adds the subscription.
     *
     * @param newSubscription the new subscription
     *
     * @return true, if successful
     */
    public
    boolean addSubscription (final PacketBuffer newSubscription) {

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
    public
    boolean hasSubscribers () {
        return !this.subscriptionBuffers.isEmpty();
    }

    /**
     * Publish.
     *
     * @param toPublish the to publish
     *
     * @return true, if successful
     */
    public
    int publish (final Packet toPublish) {
        var remainingSubscribers = this.subscriptionBuffers.size();

        for (var subscriptionBuffer : this.subscriptionBuffers) {

            if (subscriptionBuffer.appendPacket(toPublish)) {
                remainingSubscribers--;
            }
        }
        return remainingSubscribers;
    }

    /**
     * Removes the subscription.
     *
     * @param subscriptionToRemove the subscription to remove
     *
     * @return true, if successful
     */
    public
    boolean removeSubscription (final PacketBuffer subscriptionToRemove) {

        if (subscriptionToRemove != null) {
            return this.subscriptionBuffers.remove(subscriptionToRemove);
        }
        return false;
    }
}
