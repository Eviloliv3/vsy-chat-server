package de.vsy.server.data;

import de.vsy.server.service.request.CategoryIdSubscriber;
import de.vsy.shared_module.packet_exception.PacketTransmissionException;
import de.vsy.shared_module.packet_management.PacketBuffer;
import de.vsy.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.packet.property.packet_category.PacketCategory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.util.Objects.requireNonNullElseGet;

/**
 * Manages server local subscriptions.
 */
public abstract class PacketCategorySubscriptionManager {

    protected static final Logger LOGGER = LogManager.getLogger();
    private final ReadWriteLock lock;
    private final Map<PacketCategory, Map<Integer, CategoryIdSubscriber>> subscriptions;

    protected PacketCategorySubscriptionManager() {
        this(null);
    }

    /**
     * Instantiates a new PacketCategory subscription manager.
     *
     * @param subscriptions the subscriptions
     */
    protected PacketCategorySubscriptionManager(
            final Map<PacketCategory, Map<Integer, CategoryIdSubscriber>> subscriptions) {

        this.lock = new ReentrantReadWriteLock();
        this.subscriptions = subscriptions;
    }

    /**
     * Publish Packet
     *
     * @param publishedPacket the published packet
     * @throws PacketTransmissionException if no thread with Packets recipientId was found
     */
    public abstract void publish(final Packet publishedPacket) throws PacketTransmissionException;

    /**
     * Subscribe.
     *
     * @param topic              the topic name
     * @param topicId            the topic id
     * @param subscriptionBuffer the subscription buffer
     * @return the PacketBuffer
     */
    public boolean subscribe(final PacketCategory topic, final int topicId,
                             final PacketBuffer subscriptionBuffer) {
        boolean subSuccessful;
        Map<Integer, CategoryIdSubscriber> topicSubscriptions;
        CategoryIdSubscriber subscriber;
        this.lock.writeLock().lock();

        try {
            topicSubscriptions = getTopicSubscriptions(topic);
            subscriber = topicSubscriptions.getOrDefault(topicId, new CategoryIdSubscriber());
            subSuccessful = subscriber.addSubscription(subscriptionBuffer);

            if (subSuccessful) {
                LOGGER.trace("Subscription to {}/{} successful.", topic,
                        topicId);
                topicSubscriptions.put(topicId, subscriber);
                this.subscriptions.put(topic, topicSubscriptions);
            } else {
                LOGGER.warn("Subscription skipped. Client already subscribed to {}/{}/{}.", topic,
                        topicId, subscriptionBuffer);
            }
        } finally {
            this.lock.writeLock().unlock();
        }

        return subSuccessful;
    }

    /**
     * Returns the subscriptions for topic.
     *
     * @param topic the topic name
     * @return the subscriptions for topic
     */
    protected Map<Integer, CategoryIdSubscriber> getTopicSubscriptions(final PacketCategory topic) {
        this.lock.readLock().lock();

        try {
            return requireNonNullElseGet(this.subscriptions.get(topic), HashMap::new);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    /**
     * Unsubscribe.
     *
     * @param topic              the topic name
     * @param threadId           the thread id
     * @param subscriptionBuffer the subscription buffer
     * @return true, if successful
     */
    public boolean unsubscribe(final PacketCategory topic, final int threadId,
                               final PacketBuffer subscriptionBuffer) {
        boolean unsubSuccessful;
        Map<Integer, CategoryIdSubscriber> topicSubscriptions;
        CategoryIdSubscriber subscriptionBuffers;
        this.lock.writeLock().lock();

        try {
            topicSubscriptions = getTopicSubscriptions(topic);
            subscriptionBuffers = topicSubscriptions.get(threadId);

            if (subscriptionBuffers != null) {
                unsubSuccessful = subscriptionBuffers.removeSubscription(subscriptionBuffer);

                if (unsubSuccessful) {
                    if (!subscriptionBuffers.hasSubscribers()) {
                        topicSubscriptions.remove(threadId);
                    } else {
                        topicSubscriptions.put(threadId, subscriptionBuffers);
                    }
                }
            } else {
                unsubSuccessful = false;
            }
            if (unsubSuccessful) {
                LOGGER.trace("Successfully cancelled subscription to: {}/{}", topic,
                        threadId);
            } else {
                LOGGER.warn(
                        "Subscription cancellation failed. Client was not subscribed to : {}/{}/{}",
                        topic,
                        threadId, subscriptionBuffer);
            }
        } finally {
            this.lock.writeLock().unlock();
        }
        return unsubSuccessful;
    }

    public Set<Integer> checkThreadIds(final PacketCategory topic, final Set<Integer> idsToCheck) {
        final Set<Integer> foundIds = new HashSet<>(idsToCheck);
        Set<Integer> subscriberIds;
        this.lock.readLock().lock();

        try {
            subscriberIds = getThreads(topic);
            foundIds.removeIf(contactId -> !subscriberIds.contains(contactId));
            return foundIds;
        } finally {
            this.lock.readLock().unlock();
        }
    }

    /**
     * Returns the threads.
     *
     * @param topic the topic name
     * @return the threads
     */
    public Set<Integer> getThreads(final PacketCategory topic) {
        Map<Integer, CategoryIdSubscriber> topicSubscriptions;
        final Set<Integer> registeredIds = new HashSet<>();
        this.lock.readLock().lock();

        try {
            topicSubscriptions = getTopicSubscriptions(topic);
            registeredIds.addAll(topicSubscriptions.keySet());
        } finally {
            this.lock.readLock().unlock();
        }

        return registeredIds;
    }

    public abstract Set<Integer> getLocalThreads(final PacketCategory topic,
                                                 final Set<Integer> idsToCheck);
}
