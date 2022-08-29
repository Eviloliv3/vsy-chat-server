package de.vsy.chat.server.server_packet.content.builder;

import de.vsy.chat.server.server_packet.content.ServerFailureDTO;
import de.vsy.chat.server.server_packet.content.ServerPacketContentImpl;

public
class ServerFailureContentBuilder
        extends ServerPacketContentBuilder<ServerFailureContentBuilder> {

    private int failedServerId;

    public
    ServerFailureContentBuilder withFailedServerId (int failedServerId) {
        this.failedServerId = failedServerId;
        return getInstanciable();
    }

    @Override
    public
    ServerFailureContentBuilder getInstanciable () {
        return this;
    }

    @Override
    public
    ServerPacketContentImpl build () {
        return new ServerFailureDTO(this);
    }

    public
    int getFailedServerId () {
        return this.failedServerId;
    }
}
