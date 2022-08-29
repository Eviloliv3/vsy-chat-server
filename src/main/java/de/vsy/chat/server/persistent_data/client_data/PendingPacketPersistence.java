package de.vsy.chat.server.persistent_data.client_data;

import de.vsy.chat.shared_transmission.packet.Packet;

/** The Interface PendingPacketDAO. */
public
interface PendingPacketPersistence {

    /**
     * Speichert das Paket persistent, sofern es nicht bereits gespeichert wurde.
     *
     * @param toPersist Paket hinzuzufügen
     *
     * @return boolean true, if new Packetsaved
     */
    boolean persistPacket (PendingType pending, Packet toPersist);

    /**
     * Entfernt das Paket aus dem peristenten Speicher, sofern es gepeichert wurde.
     *
     * @param toRemove zu entfernendes Paket
     */
    void removePacket (PendingType pending, Packet toRemove);
}
