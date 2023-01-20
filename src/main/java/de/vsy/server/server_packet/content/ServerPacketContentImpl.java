
package de.vsy.server.server_packet.content;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.vsy.server.server_packet.content.builder.ServerPacketContentBuilder;
import de.vsy.shared_transmission.packet.content.PacketContent;

import java.io.Serial;
import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.unmodifiableSet;

/**
 * Server internal extension of PacketContent, allows for the single synchronization of servers.
 */
public abstract class ServerPacketContentImpl implements PacketContent, ServerPacketContent {

    @Serial
    private static final long serialVersionUID = -8048355680528188537L;
    private final Set<Integer> synchronizedServers;
    private final int originatingServerId;
    private int readByConnectionThread;

    /**
     * Instantiates a new server internal PacketDataManagement.
     *
     * @param builder the builder
     */
    protected ServerPacketContentImpl(ServerPacketContentBuilder<?> builder) {
        this(new HashSet<>(builder.getSynchronizedServers()), builder.getOriginatingServerId(),
                builder.getReadByConnectionThread());
    }

    @JsonCreator
    protected ServerPacketContentImpl(
            @JsonProperty("synchronizedServers") final Set<Integer> synchronizedServers,
            @JsonProperty("originatingServerId") final int originatingServerId,
            @JsonProperty("readByConnectionThread") final int readByConnectionThread) {
        this.synchronizedServers = synchronizedServers;
        this.originatingServerId = originatingServerId;
        this.readByConnectionThread = readByConnectionThread;
    }

    @Override
    public String toString() {
        final var serverInternalString = new StringBuilder();

        serverInternalString.append("\"serverInternalData\": {").append("\"synchronizedServers\": ");

        if (!this.synchronizedServers.isEmpty()) {
            final var synchronizedListBuilder = new StringBuilder();

            for (final int serverId : this.synchronizedServers) {
                synchronizedListBuilder.append(serverId).append(", ");
            }

            if (synchronizedListBuilder.length() >= 3) {
                synchronizedListBuilder.delete(synchronizedListBuilder.length() - 2,
                        synchronizedListBuilder.length() - 1);
            }
            serverInternalString.append("[").append(synchronizedListBuilder).append("]");
        } else {
            serverInternalString.append("none");
        }
        serverInternalString.append("}");
        return serverInternalString.toString();
    }

    @Override
    public void addSyncedServerId(final int serverId) {

        if (serverId > 0) {
            this.synchronizedServers.add(serverId);
        }
    }

    @Override
    public boolean checkServerSynchronizationCounter(final int checkCount) {
        return this.synchronizedServers.size() == checkCount;
    }

    @Override
    public boolean checkServerSynchronizationState(final int serverId) {
        return this.synchronizedServers.contains(serverId);
    }

    @Override
    public int getOriginatingServerId() {
        return this.originatingServerId;
    }

    /**
     * Sets the reading connection thread.
     *
     * @param readingConnectionThread the new reading connection thread
     */
    public void setReadingConnectionThread(final int readingConnectionThread) {
        if (this.readByConnectionThread == -1) {
            this.readByConnectionThread = readingConnectionThread;
        }
    }

    /**
     * Returns the synchronized servers.
     *
     * @return the synchronized servers
     */
    public Set<Integer> getSynchronizedServers() {
        return unmodifiableSet(this.synchronizedServers);
    }
}
