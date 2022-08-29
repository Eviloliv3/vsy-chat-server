package de.vsy.chat.server.server_packet.content.builder;

import de.vsy.chat.server.server_packet.content.ServerPacketContentImpl;

import java.util.HashSet;
import java.util.Set;

import static java.util.Set.copyOf;

public abstract
class ServerPacketContentBuilder<T extends ServerPacketContentBuilder<T>> {

    private final Set<Integer> synchronizedServers = new HashSet<>();
    private int readByConnectionThread = -1;
    private int originatingServerId;

    public
    int getReadByConnectionThread () {
        return this.readByConnectionThread;
    }

    public
    Set<Integer> getSyncedServers () {
        return copyOf(this.synchronizedServers);
    }

    public
    int getOriginatingServerId () {
        return this.originatingServerId;
    }

    /**
     * With.
     *
     * @param readByConnectionThread the read by connection thread
     *
     * @return the t
     */
    public
    ServerPacketContentBuilder<T> withReadByConnectionThread (
            final int readByConnectionThread) {
        this.readByConnectionThread = readByConnectionThread;
        return getInstanciable();
    }

    public abstract
    ServerPacketContentBuilder<T> getInstanciable ();

    public
    ServerPacketContentBuilder<T> withOriginatingServerId (final int originatorId) {
        this.originatingServerId = originatorId;
        return getInstanciable();
    }

    /**
     * With synchronized servers.
     *
     * @param copiedSyncSet the synchronized servers
     *
     * @return the t
     */
    public
    ServerPacketContentBuilder<T> withSyncedServers (
            final Set<Integer> copiedSyncSet) {
        if (copiedSyncSet != null && !copiedSyncSet.isEmpty()) {
            this.synchronizedServers.addAll(copiedSyncSet);
        }
        return getInstanciable();
    }

    public abstract
    ServerPacketContentImpl build ();
}
