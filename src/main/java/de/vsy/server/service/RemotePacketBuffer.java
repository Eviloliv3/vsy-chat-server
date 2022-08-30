package de.vsy.server.service;

import de.vsy.server.server_packet.content.ServerPacketContentImpl;
import de.vsy.shared_module.shared_module.packet_management.PacketBuffer;
import de.vsy.server.server.server_connection.LocalServerConnectionData;
import de.vsy.server.server.server_connection.RemoteServerConnectionData;
import de.vsy.shared_transmission.shared_transmission.packet.Packet;

/**
 * Wird zur Unterbrechung von unendlichen Klientennachrichtenketten zwischen Servern
 * genutzt.
 */
public
class RemotePacketBuffer extends PacketBuffer {

    /** Identifiziert den Server, der mittels dieses Buffers erreicht wird. */
    private final LocalServerConnectionData localConnection;
    private RemoteServerConnectionData remoteConnection;

    public
    RemotePacketBuffer (final LocalServerConnectionData localConnection,
                        final RemoteServerConnectionData remoteConnection) {
        this.localConnection = localConnection;
        this.remoteConnection = remoteConnection;
    }

    @Override
    public
    boolean appendPacket (Packet input) {
        final var content = input.getPacketContent();

        if (content instanceof final ServerPacketContentImpl serverContent) {

            if (!serverContent.checkServerSyncState(
                    this.remoteConnection.getServerId())) {
                synchronizeLocalServerId(serverContent);
                return super.appendPacket(input);
            }
        } else if (content == null) {
            return super.appendPacket(input);
        }
        return true;
    }

    @Override
    public
    boolean prependPacket (Packet input) {
        final var content = input.getPacketContent();

        if (content instanceof final ServerPacketContentImpl serverContent) {

            if (!serverContent.checkServerSyncState(
                    this.remoteConnection.getServerId())) {
                synchronizeLocalServerId(serverContent);
                return super.prependPacket(input);
            }
        } else if (content == null) {
            return super.appendPacket(input);
        }
        return true;
    }

    private
    void synchronizeLocalServerId (ServerPacketContentImpl input) {
        input.addSyncedServerId(localConnection.getServerId());
    }

    public
    void updateRemoteConnectionData (RemoteServerConnectionData updatedServerData) {
        this.remoteConnection = updatedServerData;
    }
}
