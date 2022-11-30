package de.vsy.server.service;

import de.vsy.server.data.socketConnection.LocalServerConnectionData;
import de.vsy.server.data.socketConnection.RemoteServerConnectionData;
import de.vsy.server.server_packet.content.ServerPacketContentImpl;
import de.vsy.shared_module.packet_management.PacketBuffer;
import de.vsy.shared_transmission.packet.Packet;

/**
 * Withholds Packet that already were processed by the remote server that is represented by this
 * PacketBuffer.
 */
public class RemotePacketBuffer extends PacketBuffer {

  private final LocalServerConnectionData localConnection;
  private RemoteServerConnectionData remoteConnection;

  public RemotePacketBuffer(final LocalServerConnectionData localConnection,
      final RemoteServerConnectionData remoteConnection) {
    this.localConnection = localConnection;
    this.remoteConnection = remoteConnection;
  }

  @Override
  public void appendPacket(Packet input) {
    final var content = input.getPacketContent();

    if (content instanceof final ServerPacketContentImpl serverContent) {

      if (!serverContent.checkServerSyncState(this.remoteConnection.getServerId())) {
        synchronizeLocalServerId(serverContent);
        super.appendPacket(input);
      } else {
        LOGGER.info("Packet not appended. Remote server (id: {}) already processed this packet: {}",
            this.remoteConnection.getServerId(), input);
      }
    } else {
      if (content == null) {
        super.appendPacket(input);
      } else {
        throw new IllegalArgumentException(
            "Packet not appended. Unsafe PacketContent: " + input);
      }
    }
  }

  @Override
  public void prependPacket(Packet input) {
    final var content = input.getPacketContent();

    if (content instanceof final ServerPacketContentImpl serverContent) {

      if (!serverContent.checkServerSyncState(this.remoteConnection.getServerId())) {
        synchronizeLocalServerId(serverContent);
        super.prependPacket(input);
      } else {
        LOGGER.info(
            "Packets not prepended. Remote server (id : {}) already processed this "
                + "packet: {}",
            this.remoteConnection.getServerId(), input);
      }
    } else {
      if (content == null) {
        super.appendPacket(input);
      } else {
        throw new IllegalArgumentException(
            "Packet not prepended. Unsafe PacketContent: " + input);
      }
    }
  }

  private void synchronizeLocalServerId(ServerPacketContentImpl input) {
    input.addSyncedServerId(localConnection.getServerId());
  }

  public void updateRemoteConnectionData(RemoteServerConnectionData updatedServerData) {
    this.remoteConnection = updatedServerData;
  }
}
