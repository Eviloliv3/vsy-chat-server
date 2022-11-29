package de.vsy.server.client_management;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import de.vsy.shared_transmission.packet.property.packet_category.PacketCategory;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The Class CurrentClientState.
 */
@JsonTypeName("currentClientState")
public class CurrentClientState {

  private final Map<PacketCategory, Set<Integer>> extraSubscriptions;
  private boolean pendingState;
  private boolean reconnectState;
  private ClientState currentState;
  private int serverId;

  /**
   * Instantiates a new current client state.
   *
   * @param serverId the server port
   */
  public CurrentClientState(final int serverId) {
    this(ClientState.OFFLINE, false, false, serverId, new EnumMap<>(PacketCategory.class));
  }

  /**
   * Instantiates a new current client state.
   *
   * @param currentState  the current state
   * @param pendingState  the pending state
   * @param serverId      the server port
   * @param extraChatSubs the extra chat subs
   */
  @JsonCreator
  public CurrentClientState(@JsonProperty("currentState") final ClientState currentState,
      @JsonProperty("pendingState") final boolean pendingState,
      @JsonProperty("reconnectState") final boolean reconnectState,
      @JsonProperty("serverId") final int serverId,
      @JsonProperty("extraSubscriptions") final Map<PacketCategory, Set<Integer>> extraChatSubs) {
    this.currentState = currentState;
    this.pendingState = pendingState;
    this.reconnectState = reconnectState;
    this.serverId = serverId;
    this.extraSubscriptions = extraChatSubs;
  }

  /**
   * Adds the extra subscription.
   *
   * @param topicName the topic name
   * @param threadId  the thread id
   */
  public void updateExtraSubscription(final PacketCategory topicName, final int threadId,
      final boolean toAdd) {
    var threadIdSet = this.extraSubscriptions.get(topicName);

    if (threadIdSet == null) {
      threadIdSet = new HashSet<>();
    }

    if (toAdd) {
      threadIdSet.add(threadId);
    } else {
      threadIdSet.remove(threadId);
    }
    this.extraSubscriptions.put(topicName, threadIdSet);
  }

  /**
   * Change server port.
   *
   * @param serverId the server port
   */
  public void changeServerPort(final int serverId) {
    this.serverId = serverId;
  }

  /**
   * Gets the current state.
   *
   * @return the current state
   */
  public ClientState getCurrentState() {
    return this.currentState;
  }

  /**
   * Sets the current state.
   *
   * @param newState the new current state
   */
  public void setCurrentState(final ClientState newState) {
    this.currentState = newState;
  }

  /**
   * Gets the extra subscriptions.
   *
   * @return the extra subscriptions
   */
  public Map<PacketCategory, Set<Integer>> getExtraSubscriptions() {
    return this.extraSubscriptions;
  }

  /**
   * Gets the pending state.
   *
   * @return the pending state
   */
  public boolean getPendingState() {
    return this.reconnectState;
  }

  /**
   * Gets the reconnection state.
   *
   * @return the reconnection state
   */
  public boolean getReconnectState() {
    return this.reconnectState;
  }

  /**
   * Gets the server port.
   *
   * @return the server port
   */
  public int getServerId() {
    return this.serverId;
  }

  /**
   * Schwebezustand eines Klienten wird gesetzt. Der Wiederverbindungszustand wird zurückgesetzt.
   * Ein verbundener Klient (pending == false) kann nicht wiederverbunden werden. Ein frisch
   * schwebender (pending == true) kann nicht gleichzeitig wiederverbunden werden.
   *
   * @param pendingState the new pending state
   */
  public void setPendingState(final boolean pendingState) {
    this.pendingState = pendingState;
  }

  /**
   * Sets global client reconnect state true or false if reconnect state is false and pending is also true.
   * @param reconnectState the desired global client reconnect state
   * @return true, if argument is false or global reconnect state is false and global pending is true; false otherwise
   */
  public boolean setReconnectState(boolean reconnectState) {
    final var reconnectSet = true;

    if (reconnectState && (this.pendingState && !this.reconnectState)) {
      this.reconnectState = true;
      return reconnectSet;
    } else {
      if (!reconnectState) {
        this.reconnectState = false;
        return reconnectSet;
      }
    }
    return !reconnectSet;
  }
}
