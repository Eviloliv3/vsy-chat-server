/*
 *
 */
package de.vsy.server.server_packet.content;

/**
 * Interface enabling simple inter server status Packet synchronization.
 */
public interface ServerStatusSync {

    /**
     * Adds the synchronized server id.
     *
     * @param serverId the server id
     */
    void addSyncedServerId(int serverId);

    /**
     * Check server sync count.
     *
     * @param checkCount the check count
     * @return true, if successful
     */
    boolean checkServerSyncCount(int checkCount);

    /**
     * Check server sync state.
     *
     * @param serverId the server id
     * @return true, if successful
     */
    boolean checkServerSyncState(int serverId);

    int getOriginatingServerId();
}
