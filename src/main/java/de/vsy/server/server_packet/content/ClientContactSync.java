package de.vsy.server.server_packet.content;

import java.util.Set;

/**
 * Schnittstelle zur Klientenstatussynchronisation. Synchronisationspaket soll
 * auch ein Paket zur Synchronisation von betroffenen Klienten enthalten.
 */
public interface ClientContactSync {

	/**
	 * Gets the contact id list.
	 *
	 * @return the contact id list
	 */
	Set<Integer> getContactIdSet();

	/**
	 * Sets the remaining contact idlist.
	 *
	 * @param remainingContacts the new remaining contact idlist
	 */
	void setRemainingContactIds(Set<Integer> remainingContacts);
}
