package de.vsy.server.service;

import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import de.vsy.server.server.data.AbstractPacketCategorySubscriptionManager;
import de.vsy.shared_transmission.shared_transmission.packet.property.communicator.EligibleCommunicationEntity;

/** Verwaltungseinheit für alle Abonnements, service- und klientseitig. */
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
	 *
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
	 * Gets the subscriptions manager.
	 *
	 * @param entity the entity
	 *
	 * @return the subscriptions manager
	 */
	public AbstractPacketCategorySubscriptionManager getSubscriptionsManager(final EligibleCommunicationEntity entity) {
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
	 *
	 * @return true, if successful
	 */
	public boolean removeCommunicationNetwork(final EligibleCommunicationEntity entity) {
		return this.communicationNetworks.remove(entity) != null;
	}
}
