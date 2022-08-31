package de.vsy.server.server.client_management;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import de.vsy.shared_transmission.shared_transmission.packet.property.packet_category.PacketCategory;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** The Class CurrentClientState. */
@JsonTypeName("currentClientState")
public
class CurrentClientState {

    private final Map<PacketCategory, Set<Integer>> extraSubscriptions;
    private final PendingState pendingState;
    private ClientState currentState;
    private int serverId;

    /**
     * Instantiates a new current client state.
     *
     * @param serverId the server port
     */
    public
    CurrentClientState (final int serverId) {
        this(ClientState.OFFLINE, new PendingState(), serverId,
             new EnumMap<>(PacketCategory.class));
    }

    /**
     * Instantiates a new current client state.
     *
     * @param currentState the current state
     * @param pendingState the pending state
     * @param serverId the server port
     * @param extraChatSubs the extra chat subs
     */
    @JsonCreator
    public
    CurrentClientState (@JsonProperty("currentState") final ClientState currentState,
                        @JsonProperty("pendingState")
                        final PendingState pendingState,
                        @JsonProperty("serverId") final int serverId,
                        @JsonProperty("extraSubscriptions")
                        final Map<PacketCategory, Set<Integer>> extraChatSubs) {
        this.currentState = currentState;
        this.pendingState = pendingState;
        this.serverId = serverId;
        this.extraSubscriptions = extraChatSubs;
    }

    /**
     * Adds the extra subscription.
     *
     * @param topicName the topic name
     * @param threadId the thread id
     */
    public
    void updateExtraSubscription (final PacketCategory topicName, final int threadId,
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
    public
    void changeServerPort (final int serverId) {
        this.serverId = serverId;
    }

    /**
     * Gets the current state.
     *
     * @return the current state
     */
    public
    ClientState getCurrentState () {
        return this.currentState;
    }

    /**
     * Sets the current state.
     *
     * @param newState the new current state
     */
    public
    void setCurrentState (final ClientState newState) {
        this.currentState = newState;
    }

    /**
     * Gets the extra subscriptions.
     *
     * @return the extra subscriptions
     */
    public
    Map<PacketCategory, Set<Integer>> getExtraSubscriptions () {
        return this.extraSubscriptions;
    }

    /**
     * Gets the pending state.
     *
     * @return the pending state
     */
    @JsonIgnore
    public
    boolean getPendingFlag () {
        return this.pendingState.getPendingState();
    }

    /**
     * Sets the pending flag.
     *
     * @param newFlag the new pending flag
     */
    public
    void setPendingFlag (final boolean newFlag) {
        this.pendingState.setPendingState(newFlag);
    }

    /**
     * Gets the pending flag.
     *
     * @return the pending flag
     */
    public
    PendingState getPendingState () {
        return this.pendingState;
    }

    /**
     * Gets the reconnection permit.
     *
     * @return the reconnection permit
     */
    public
    boolean setReconnectionState (final boolean newState) {
        return this.pendingState.setReconnecting(newState);
    }

    /**
     * Gets the reconnection state.
     *
     * @return the reconnection state
     */
    @JsonIgnore
    public
    boolean getReconnectionState () {
        return this.pendingState.getReconnecting();
    }

    /**
     * Gets the server port.
     *
     * @return the server port
     */
    public
    int getServerId () {
        return this.serverId;
    }
}
