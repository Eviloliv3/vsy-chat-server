/**  */
package de.vsy.chat.server.server.data.access;

import de.vsy.chat.shared_module.data_element_validation.IdCheck;
import de.vsy.chat.server.server.client_management.ClientState;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * The Class ClientStateSubscriptions.
 *
 * @author Frederic Heath
 */
public
class ClientStateSubscriptions {

    private final Map<Integer, Map<ClientState, Set<Integer>>> clientSubscriptions;
    private final ReadWriteLock lock;

    /** Instantiates a new client state subscriptions. */
    public
    ClientStateSubscriptions () {
        this.lock = new ReentrantReadWriteLock();
        this.clientSubscriptions = new HashMap<>();
    }

    /**
     * Gets the client subscriptions.
     *
     * @param state the state
     * @param clientId the client id
     *
     * @return the client subscriptions
     */
    public
    Set<Integer> getClientSubscriptions (final ClientState state,
                                         final int clientId) {
        final Set<Integer> subscribedClients = new HashSet<>();

        if (state != null && IdCheck.checkData(clientId) == null) {
            Map<ClientState, Set<Integer>> subsPerState;

            try {
                this.lock.readLock().lock();
                subsPerState = this.clientSubscriptions.get(clientId);

                if (subsPerState != null) {
                    final var subList = subsPerState.get(state);

                    if (subList != null) {
                        subscribedClients.addAll(subList);
                    }
                }
            } finally {
                this.lock.readLock().unlock();
            }
        }
        return subscribedClients;
    }

    /**
     * Subscribe.
     *
     * @param state the state
     * @param clientId the client id
     * @param contactId the contact id
     *
     * @return true, if successful
     */
    public
    boolean subscribe (final ClientState state, final int clientId,
                       final int contactId) {
        var subSuccessful = false;

        if (state != null && IdCheck.checkData(clientId) == null &&
            IdCheck.checkData(contactId) == null) {
            Map<ClientState, Set<Integer>> subsPerState;
            Set<Integer> subscribedClients;

            try {
                this.lock.writeLock().lock();
                subsPerState = this.clientSubscriptions.get(contactId);

                if (subsPerState == null) {
                    subsPerState = new EnumMap<>(ClientState.class);
                }
                subscribedClients = subsPerState.get(state);

                if (subscribedClients == null) {
                    subscribedClients = new HashSet<>();
                }

                subSuccessful = subscribedClients.add(clientId);
                if (subSuccessful) {
                    subsPerState.put(state, subscribedClients);
                }
                this.clientSubscriptions.put(contactId, subsPerState);
            } finally {
                this.lock.writeLock().unlock();
            }
        }
        return subSuccessful;
    }

    /**
     * Unsubscribe.
     *
     * @param state the state
     * @param clientId the client id
     * @param contactId the contact id
     *
     * @return true, if successful
     */
    public
    boolean unsubscribe (final ClientState state, final int clientId,
                         final int contactId) {
        var unsubSuccessful = false;

        if (state != null && IdCheck.checkData(clientId) == null &&
            IdCheck.checkData(contactId) == null) {
            Map<ClientState, Set<Integer>> subsPerState;

            try {
                this.lock.writeLock().lock();
                subsPerState = this.clientSubscriptions.get(contactId);

                if (subsPerState != null) {
                    final var subscribedClients = subsPerState.get(state);

                    if (subscribedClients != null) {
                        unsubSuccessful = subscribedClients.remove(clientId);

                        if (!subscribedClients.isEmpty()) {
                            subsPerState.put(state, subscribedClients);
                            this.clientSubscriptions.put(contactId, subsPerState);
                        } else {
                            this.clientSubscriptions.remove(contactId);
                        }
                    }
                }
            } finally {
                this.lock.writeLock().unlock();
            }
        }
        return unsubSuccessful;
    }
}
