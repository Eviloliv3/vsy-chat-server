package de.vsy.server.service.request;

import de.vsy.shared_transmission.packet.Packet;

public interface PublishablePacketCreator {

    /**
     * Specifically checks for ExtendedStatusSyncDTO carrying Packets and creates
     * simple status messages for locally connected clients, depending on the declared eligible recipients.
     *
     * @param input the Packet
     * @return the Packet, if it is not distributable; null, if distributable, but no
     * unsynchronized servers are left.
     */
    Packet handleDistributableContent(Packet input);
}
