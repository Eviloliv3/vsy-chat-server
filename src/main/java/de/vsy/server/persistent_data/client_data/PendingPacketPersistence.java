package de.vsy.server.persistent_data.client_data;

import de.vsy.shared_transmission.packet.Packet;

/**
 * The Interface PendingPacketDAO.
 */
public interface PendingPacketPersistence {

    /**
     * Saves unique Packets.
     *
     * @param toPersist Packet
     * @return boolean true, if Packet saved
     */
    boolean persistPacket(PendingType pending, Packet toPersist);

    /**
     * Removes Packets.
     *
     * @param pending  PendingType
     * @param toRemove Packet
     */
    void removePacket(PendingType pending, Packet toRemove);
}
