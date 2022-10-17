package de.vsy.server.server.server_connection;

import de.vsy.server.service.RemotePacketBuffer;
import de.vsy.shared_module.shared_module.packet_management.PacketBuffer;
import java.io.IOException;
import java.net.Socket;
import java.util.Objects;

public class RemoteServerConnectionData implements ServerConnectionDataProvider {

  private final int remoteServerId;
  private final boolean leaderFlag;
  private final Socket followerSocket;
  private RemotePacketBuffer remoteServerConnector;

  private RemoteServerConnectionData(final int remoteServerId, final boolean leaderFlag,
      final Socket followerSocket) {
    this.remoteServerId = remoteServerId;
    this.leaderFlag = leaderFlag;
    this.followerSocket = followerSocket;
  }

  public static RemoteServerConnectionData valueOf(final int remoteServerId,
      final boolean leaderFlag,
      final Socket followerSocket) {
    if (followerSocket == null) {
      throw new IllegalArgumentException("Kein Socket angegeben.");
    }
    return new RemoteServerConnectionData(remoteServerId, leaderFlag, followerSocket);
  }

  public void setRemoteServerConnector(final RemotePacketBuffer buffer) {
    this.remoteServerConnector = buffer;
  }

  public PacketBuffer getRemoteServerBuffer() {
    return this.remoteServerConnector;
  }

  @Override
  public String getHostname() {
    return this.followerSocket.getInetAddress().getHostName();
  }

  @Override
  public int getServerPort() {
    return this.followerSocket.getLocalPort();
  }

  @Override
  public int getServerId() {
    return this.remoteServerId;
  }

  @Override
  public boolean closeConnection() throws IOException {
    this.followerSocket.close();
    return this.followerSocket.isClosed();
  }

  @Override
  public int hashCode() {
    var hash = 97 * Integer.hashCode(this.remoteServerId);
    hash = hash * 97 * this.followerSocket.hashCode();

    if (this.remoteServerConnector != null) {
      hash = hash * 97 * this.remoteServerConnector.hashCode();
    } else {
      hash = hash * Objects.hashCode(null);
    }
    return hash * 97 * Boolean.hashCode(this.leaderFlag);
  }

  @Override
  public boolean equals(Object otherObject) {
    if (this == otherObject) {
      return true;
    }

    if (otherObject instanceof final RemoteServerConnectionData otherConnection) {
      return this.leaderFlag == otherConnection.isLeader()
          && this.remoteServerId == otherConnection.getServerId()
          && this.followerSocket.equals(otherConnection.getConnectionSocket());
    }
    return false;
  }

  public boolean isLeader() {
    return this.leaderFlag;
  }

  public Socket getConnectionSocket() {
    return this.followerSocket;
  }
}
