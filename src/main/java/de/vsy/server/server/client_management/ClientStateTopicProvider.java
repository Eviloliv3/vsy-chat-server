/*
 *
 */
package de.vsy.server.server.client_management;

import java.util.EnumMap;
import java.util.Set;

import static java.util.Set.of;

/**
 * Server internal representation of ClientStates and the respectively expected
 * Sender CommunicationEndpointId types. A client is not allowed to send requests
 * with a String as CommunicationEndpointId after he was labelled as AUTHENTICATED.
 * He is expected to use his clientId.
 */
public
class ClientStateTopicProvider {

    protected static final EnumMap<ClientState, ClientStateTopicProvider> stateTopicAssignment;
    private static final ClientStateTopicProvider MESSENGER;
    private static final ClientStateTopicProvider AUTHENTICATION;
    private final Set<ClientState> subscriptionDependencies;
    private final Set<ClientState> unsubscriptionDependencies;

    static {
        MESSENGER = new ClientStateTopicProvider(
                of(ClientState.AUTHENTICATED, ClientState.ACTIVE_MESSENGER),
                Set.of(ClientState.ACTIVE_MESSENGER));
        AUTHENTICATION = new ClientStateTopicProvider(
                Set.of(ClientState.AUTHENTICATED),
                of(ClientState.ACTIVE_MESSENGER, ClientState.AUTHENTICATED));

        stateTopicAssignment = new EnumMap<>(ClientState.class);
        stateTopicAssignment.put(ClientState.ACTIVE_MESSENGER, MESSENGER);
        stateTopicAssignment.put(ClientState.AUTHENTICATED, AUTHENTICATION);
    }

    private
    ClientStateTopicProvider (final Set<ClientState> subscriptionDependencies,
                              final Set<ClientState> unsubscriptionDependencies) {
        this.subscriptionDependencies = subscriptionDependencies;
        this.unsubscriptionDependencies = unsubscriptionDependencies;
    }

    public
    Set<ClientState> getDependencies (final boolean isSubscription) {

        if (isSubscription) {
            return this.subscriptionDependencies;
        } else {
            return this.unsubscriptionDependencies;
        }
    }
}
