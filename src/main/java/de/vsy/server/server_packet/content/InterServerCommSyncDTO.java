/*
 *
 */
package de.vsy.server.server_packet.content;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serial;
import java.util.HashSet;
import java.util.Set;

/**
 * Contains local server's data to be transferred to another server, for synchronization purposes.
 */
public class InterServerCommSyncDTO extends ServerPacketContentImpl {

  @Serial
  private static final long serialVersionUID = -2653392996505694664L;
  private final int serverId;

  /**
   * Instantiates a new client status sync dataManagement.
   *
   * @param serverId the server port
   */
  public InterServerCommSyncDTO(final int serverId) {
    this(new HashSet<>(), serverId, -1, serverId);
  }

  @JsonCreator
  public InterServerCommSyncDTO(
      @JsonProperty("synchronizedServers") final Set<Integer> synchronizedServers,
      @JsonProperty("originatingServerId") final int originatingServerId,
      @JsonProperty("readByConnectionThread") final int readByConnectionThread,
      @JsonProperty("serverId") final int serverId) {
    super(synchronizedServers, originatingServerId, readByConnectionThread);
    this.serverId = serverId;
  }

  @Override
  public String toString() {
    return "\"interServerCommSync\" : { " + super.toString() + ", " + "serverId: " + this.serverId
        + " }";
  }

  /**
   * Returns the server port.
   *
   * @return the server port
   */
  public int getServerId() {
    return this.serverId;
  }
}
