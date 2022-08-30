/*
 *
 */
package de.vsy.server.server_packet.content;

import de.vsy.server.server_packet.content.builder.ServerFailureContentBuilder;

import java.io.Serial;

/**
 * Enthaelt Informationen über Serverfehler, die intern verarbeitet werden müssen.
 */
public
class ServerFailureDTO extends ServerPacketContentImpl {

    @Serial
    private static final long serialVersionUID = 8143478753388566768L;
    public final int failedServerId;

    /**
     * Instantiates a new server failure dataManagement.
     *
     * @param builder the builder
     */
    public
    ServerFailureDTO (ServerFailureContentBuilder builder) {
        super(builder);
        this.failedServerId = builder.getFailedServerId();
    }

    public
    int getFailedServerId () {
        return this.failedServerId;
    }

    @Override
    public
    String toString () {
        return super.toString() + "\"serverFailureData\": {}";
    }
}
