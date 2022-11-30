package de.vsy.server.data;

import static java.util.Objects.requireNonNullElseGet;

import de.vsy.server.service.request.CategoryIdSubscriber;
import de.vsy.shared_module.packet_exception.PacketTransmissionException;
import de.vsy.shared_module.packet_management.PacketBuffer;
import de.vsy.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.packet.property.packet_category.PacketCategory;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Manages server local subscriptions.
 */
public abstract class AbstractPacketCategorySubscriptionManager {

  protected static final Logger LOGGER = LogManager.getLogger();
  private final ReadWriteLock lock;
  private final Map<PacketCategory, Map<Integer, CategoryIdSubscriber>> subscriptions;

  protected AbstractPacketCategorySubscriptionManager() {
    this(null);
  }

  /**
   * Instantiates a new PacketCategory subscription manager.
   *
   * @param subscriptions the subscriptions
   */
  protected AbstractPacketCategorySubscriptionManager(
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

    try {
      this.lock.writeLock().lock();

      topicSubscriptions = getTopicSubscriptions(topic);
      subscriber = topicSubscriptions.getOrDefault(topicId, new CategoryIdSubscriber());
      subSuccessful = subscriber.addSubscription(subscriptionBuffer);

      if (subSuccessful) {
        LOGGER.trace("Subscription to topic/thread {}{} successful", topic,
            topicId);
        topicSubscriptions.put(topicId, subscriber);
        this.subscriptions.put(topic, topicSubscriptions);
      } else {
        LOGGER.warn("Subscription failed. Client already subscribed to topic/thread {}/{}", topic,
            topicId);
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

    try {
      this.lock.readLock().lock();
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

    try {
      this.lock.writeLock().lock();

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
        LOGGER.trace("Subscription cancellation of topic/thread successful: {}/{}", topic, threadId);
      } else {
        LOGGER.warn(
            "Subscription cancellation failed. Client was not subscribed to topic/thread: {}/{}",
            topic,
            threadId);
      }
    } finally {
      this.lock.writeLock().unlock();
    }
    return unsubSuccessful;
  }

  public Set<Integer> checkThreadIds(final PacketCategory topic, final Set<Integer> idsToCheck) {
    final Set<Integer> foundIds = new HashSet<>();
    Set<Integer> subscriberIds;

    try {
      this.lock.readLock().lock();
      subscriberIds = getThreads(topic);

      for (var checkedId : idsToCheck) {

        if (subscriberIds.contains(checkedId)) {
          foundIds.add(checkedId);
        }
      }
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

    try {
      this.lock.readLock().lock();
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
