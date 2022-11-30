package de.vsy.server.service;

import de.vsy.server.data.AbstractPacketCategorySubscriptionManager;
import de.vsy.shared_transmission.packet.property.communicator.EligibleCommunicationEntity;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Manages subscriptions for publish/subscribe system.
 */
public class CommunicationNetworkSubscriptionManager {

  private final ReadWriteLock lock;
  private final Map<EligibleCommunicationEntity, AbstractPacketCategorySubscriptionManager> communicationNetworks;

  /**
   * Instantiates a new communication network subscription manager.
   *
   * @param communicationNetworks the communication networks
   */
  public CommunicationNetworkSubscriptionManager(
      final Map<EligibleCommunicationEntity, AbstractPacketCategorySubscriptionManager> communicationNetworks) {
    this.lock = new ReentrantReadWriteLock();
    this.communicationNetworks = communicationNetworks;
  }

  /**
   * Adds the communication network.
   *
   * @param entity              the entity
   * @param subscriptionManager the subscription manager
   * @return true, if successful
   */
  public boolean addCommunicationNetwork(final EligibleCommunicationEntity entity,
      final AbstractPacketCategorySubscriptionManager subscriptionManager) {

    try {
      this.lock.writeLock().lock();

      return this.communicationNetworks.putIfAbsent(entity, subscriptionManager) == null;
    } finally {
      this.lock.writeLock().unlock();
    }
  }

  /**
   * Returns the subscription manager.
   *
   * @param entity the entity
   * @return AbstractPacketCategorySubscriptionManager
   */
  public AbstractPacketCategorySubscriptionManager getSubscriptionsManager(
      final EligibleCommunicationEntity entity) {
    AbstractPacketCategorySubscriptionManager subscriptionManager;

    try {
      this.lock.readLock().lock();
      subscriptionManager = this.communicationNetworks.get(entity);
    } finally {
      this.lock.readLock().unlock();
    }

    return subscriptionManager;
  }

  /**
   * Removes the communication network.
   *
   * @param entity the entity
   * @return true, if successful
   */
  public boolean removeCommunicationNetwork(final EligibleCommunicationEntity entity) {
    return this.communicationNetworks.remove(entity) != null;
  }
}
