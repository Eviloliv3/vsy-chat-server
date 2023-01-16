package de.vsy.server.client_management;

import de.vsy.shared_transmission.packet.property.packet_category.PacketCategory;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static de.vsy.shared_transmission.packet.property.packet_category.PacketCategory.*;
import static java.util.Collections.emptyMap;
import static java.util.Set.of;

/**
 * Translates client state changes into client subscriptions.
 */
public class ClientStateTranslator {

    private static final Map<ClientState, Set<PacketCategory>> CLIENT_STATE_TO_CATEGORY_SUBSCRIPTIONS;

    static {
        CLIENT_STATE_TO_CATEGORY_SUBSCRIPTIONS = new EnumMap<>(ClientState.class);
        CLIENT_STATE_TO_CATEGORY_SUBSCRIPTIONS.put(ClientState.AUTHENTICATED,
                of(NOTIFICATION, RELATION));
        CLIENT_STATE_TO_CATEGORY_SUBSCRIPTIONS.put(ClientState.ACTIVE_MESSENGER, of(CHAT, STATUS));
    }

    /**
     * Instantiates a new state to subscription translator.
     */
    private ClientStateTranslator() {
    }

    /**
     * Prepares a Set of ClientStates that a given ClientSate depends on considering
     * their associated subscriptions. Then calls createTopicThreadMap(..) using the
     * dependent ClientState Set to create a Map of topics and threads per topic to
     * (not) be subscribed to for a  given ClientState and removal flag.
     *
     * @param clientState    the ClientState as evaluation base
     * @param onlineStatus   the (un-)subscription flag
     * @param communicatorId the id to be used as basic thread
     * @return Map of topic - threads
     */
    public static Map<PacketCategory, Set<Integer>> prepareClientSubscriptionMap(
            final ClientState clientState,
            final boolean onlineStatus, final int communicatorId) {
        Map<PacketCategory, Set<Integer>> requestedMapping = null;
        final var dependingStateProvider = DependentClientStateProvider.getDependentStateProvider(
                clientState);

        if (dependingStateProvider != null) {
            final var dependingStates = dependingStateProvider.getDependentStatesForSubscription(onlineStatus);
            requestedMapping = createTopicThreadMap(communicatorId, dependingStates);
        }
        return requestedMapping != null ? requestedMapping : emptyMap();
    }

    /**
     * Creates a Map of topics and threads per topic to (not) be subscribed to for a
     * given ClientState and removal flag.
     *
     * @param clientStateSet Set of
     * @param communicatorId the id to be used as basic thread
     * @return Map of topic - threads
     */
    private static Map<PacketCategory, Set<Integer>> createTopicThreadMap(final int communicatorId,
                                                                          Set<ClientState> clientStateSet) {
        final var topicThreadMap = new EnumMap<PacketCategory, Set<Integer>>(PacketCategory.class);

        for (final var currentState : clientStateSet) {
            final var topics = CLIENT_STATE_TO_CATEGORY_SUBSCRIPTIONS.get(currentState);
            final var threadSet = new HashSet<Integer>();

            for (final var currentTopic : topics) {
                threadSet.add(communicatorId);
                topicThreadMap.put(currentTopic, threadSet);
            }
        }
        return topicThreadMap;
    }
}
