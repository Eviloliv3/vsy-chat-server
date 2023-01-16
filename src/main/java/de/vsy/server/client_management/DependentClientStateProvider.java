/*
 *
 */
package de.vsy.server.client_management;

import java.util.EnumMap;
import java.util.Set;
import java.util.function.Supplier;

import static java.util.Set.of;

/**
 * Server internal representation of ClientStates and the respectively expected Sender
 * CommunicationEndpointId types. A client is not allowed to send requests with a String as
 * CommunicationEndpointId after he was labelled as AUTHENTICATED. He is expected to use his
 * clientId.
 */
public class DependentClientStateProvider {

    public static final EnumMap<ClientState, Supplier<DependentClientStateProvider>> stateTopicAssignment;
    private final Set<ClientState> subscriptionDependencies;
    private final Set<ClientState> unsubscriptionDependencies;

    static {
        stateTopicAssignment = new EnumMap<>(ClientState.class);
        stateTopicAssignment.put(ClientState.ACTIVE_MESSENGER, DependentClientStateProvider::getMessengerDependentStateProvider);
        stateTopicAssignment.put(ClientState.AUTHENTICATED, DependentClientStateProvider::getAuthenticationStateDependentStateProvider);
    }

    private DependentClientStateProvider(final Set<ClientState> subscriptionDependencies,
                                         final Set<ClientState> unsubscriptionDependencies) {
        this.subscriptionDependencies = subscriptionDependencies;
        this.unsubscriptionDependencies = unsubscriptionDependencies;
    }

    private static DependentClientStateProvider getMessengerDependentStateProvider(){
        return new DependentClientStateProvider(
                of(ClientState.AUTHENTICATED, ClientState.ACTIVE_MESSENGER),
                Set.of(ClientState.ACTIVE_MESSENGER));
    }

    private static DependentClientStateProvider getAuthenticationStateDependentStateProvider(){
        return new DependentClientStateProvider(Set.of(ClientState.AUTHENTICATED),
                of(ClientState.ACTIVE_MESSENGER, ClientState.AUTHENTICATED));
    }

    public static DependentClientStateProvider getDependentStateProvider(final ClientState state){
        return DependentClientStateProvider.getDependentStateProvider(state);
    }

    public Set<ClientState> getDependentStatesForSubscription(final boolean isSubscription) {

        if (isSubscription) {
            return this.subscriptionDependencies;
        } else {
            return this.unsubscriptionDependencies;
        }
    }
}
