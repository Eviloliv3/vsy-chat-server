package de.vsy.server.server_packet.content;

/**
 * Interface enabling simple inter server status Packet synchronization.
 */
public interface ServerPacketContent {

    /**
     * Adds the server's id to the Collection of servers that already processed this
     * ServerPacketContent.
     *
     * @param serverId the server's id
     */
    void addSyncedServerId(int serverId);

    /**
     * Returns if the Collection size of synchronized servers equals the specified
     * count.
     *
     * @param checkCount the count to check
     * @return true, if count equals count of synchronized servers.
     */
    boolean checkServerSynchronizationCounter(int checkCount);

    /**
     * Check whether a server already marked this content as processed.
     *
     * @param serverId the server id
     * @return true, if successful
     */
    boolean checkServerSynchronizationState(int serverId);

    /**
     * Returns the first server to create this ServerPacketContent.
     *
     * @return originating server's id as int
     */
    int getOriginatingServerId();
}
