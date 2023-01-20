
package de.vsy.server.client_management;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;

/**
 * Server internal representation of ClientStates and the respectively expected Sender
 * CommunicationEndpointId types. A client is not allowed to send requests with a String as
 * CommunicationEndpointId after he was labelled as AUTHENTICATED. He is expected to use his
 * clientId.
 */
public class DependentClientStateProvider {

    public static final EnumMap<ClientState, Supplier<DependentClientStateProvider>> stateTopicAssignment;

    static {
        stateTopicAssignment = new EnumMap<>(ClientState.class);
        stateTopicAssignment.put(ClientState.ACTIVE_MESSENGER, DependentClientStateProvider::getMessengerDependentStateProvider);
        stateTopicAssignment.put(ClientState.AUTHENTICATED, DependentClientStateProvider::getAuthenticationStateDependentStateProvider);
    }

    private final List<ClientState> subscriptionDependencies;
    private final List<ClientState> unsubscriptionDependencies;

    private DependentClientStateProvider(final List<ClientState> subscriptionDependencies,
                                         final List<ClientState> unsubscriptionDependencies) {
        this.subscriptionDependencies = subscriptionDependencies;
        this.unsubscriptionDependencies = unsubscriptionDependencies;
    }

    private static DependentClientStateProvider getMessengerDependentStateProvider() {
        return new DependentClientStateProvider(
                List.of(ClientState.AUTHENTICATED, ClientState.ACTIVE_MESSENGER),
                List.of(ClientState.ACTIVE_MESSENGER));
    }

    private static DependentClientStateProvider getAuthenticationStateDependentStateProvider() {
        return new DependentClientStateProvider(List.of(ClientState.AUTHENTICATED),
                List.of(ClientState.ACTIVE_MESSENGER, ClientState.AUTHENTICATED));
    }

    public static DependentClientStateProvider getDependentStateProvider(final ClientState state) {
        return DependentClientStateProvider.stateTopicAssignment.get(state).get();
    }

    public List<ClientState> getDependentStatesForSubscription(final boolean isSubscription) {

        if (isSubscription) {
            return this.subscriptionDependencies;
        } else {
            return this.unsubscriptionDependencies;
        }
    }
}
