/*
 *
 */
package de.vsy.chat.server.server_packet.content;

import java.io.Serial;
import java.util.HashSet;

/** Paketdaten zur erstmaligen Synchronisation eines zweiten Servers. */
public
class InterServerCommSyncDTO extends ServerPacketContentImpl {

    @Serial
    private static final long serialVersionUID = -2653392996505694664L;
    private final int serverId;

    /**
     * Instantiates a new client status sync dataManagement.
     *
     * @param serverId the server port
     */
    public
    InterServerCommSyncDTO (final int serverId) {
        super(new HashSet<>(), serverId, -1);
        this.serverId = serverId;
    }

    @Override
    public
    String toString () {
        return "\"interServerCommSync\" : { " + super.toString() + ", " +
               "serverId: " + this.serverId + " }";
    }

    /**
     * Gets the server port.
     *
     * @return the server port
     */
    public
    int getServerId () {
        return this.serverId;
    }
}
