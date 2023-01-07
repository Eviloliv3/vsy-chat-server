package de.vsy.server.server_packet.content;

import java.util.Set;

/**
 * Interface allowing for client state synchronization.
 */
public interface ClientContactSync {

    /**
     * Returns the contact id list.
     *
     * @return the contact id list
     */
    Set<Integer> getContactIdSet();

    /**
     * Sets the remaining id set.
     *
     * @param remainingContacts Set<Integer>
     */
    void setRemainingContactIds(Set<Integer> remainingContacts);
}
