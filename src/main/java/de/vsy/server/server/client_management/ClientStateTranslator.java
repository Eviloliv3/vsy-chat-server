package de.vsy.server.server.client_management;

import static de.vsy.shared_transmission.shared_transmission.packet.property.packet_category.PacketCategory.CHAT;
import static de.vsy.shared_transmission.shared_transmission.packet.property.packet_category.PacketCategory.ERROR;
import static de.vsy.shared_transmission.shared_transmission.packet.property.packet_category.PacketCategory.RELATION;
import static de.vsy.shared_transmission.shared_transmission.packet.property.packet_category.PacketCategory.STATUS;
import static java.util.Collections.emptyMap;
import static java.util.Set.of;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.vsy.shared_transmission.shared_transmission.packet.property.packet_category.PacketCategory;

/** Uebernimmt das Uebersetzen eines Klientenzustandes in Abonnements. */
public class ClientStateTranslator {

	private static final Map<ClientState, Set<PacketCategory>> CLIENT_STATE_TO_CATEGORY_SUBSCRIPTIONS;

	static {
		CLIENT_STATE_TO_CATEGORY_SUBSCRIPTIONS = new EnumMap<>(ClientState.class);
		CLIENT_STATE_TO_CATEGORY_SUBSCRIPTIONS.put(ClientState.AUTHENTICATED, of(STATUS, ERROR, RELATION));
		CLIENT_STATE_TO_CATEGORY_SUBSCRIPTIONS.put(ClientState.ACTIVE_MESSENGER, of(CHAT));
	}

	/** Instantiates a new state to subscription translator. */
	private ClientStateTranslator() {
	}

	/**
	 * Translate state.
	 *
	 * @param clientState    the client state
	 * @param onlineStatus   the online status
	 * @param communicatorId the communicator id
	 *
	 * @return true if (un-)subscribed
	 */
	public static Map<PacketCategory, Set<Integer>> prepareClientSubscriptionMap(final ClientState clientState,
			final boolean onlineStatus, final int communicatorId) {
		Map<PacketCategory, Set<Integer>> requestedMapping = null;
		final var dependingStateProvider = ClientStateTopicProvider.stateTopicAssignment.get(clientState);

		if (dependingStateProvider != null) {
			final var dependingStates = dependingStateProvider.getDependencies(onlineStatus);
			requestedMapping = createTopicThreadMap(communicatorId, dependingStates);
		}
		return requestedMapping != null ? requestedMapping : emptyMap();
	}

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
