package de.vsy.server.client_handling.data_management.logic;

import de.vsy.server.server.data.AbstractPacketCategorySubscriptionManager;
import de.vsy.shared_module.shared_module.packet_management.ThreadPacketBufferLabel;
import de.vsy.shared_module.shared_module.packet_management.ThreadPacketBufferManager;
import de.vsy.server.client_handling.data_management.ExtraClientSubscriptionProvider;
import de.vsy.server.client_handling.data_management.bean.ClientStateListener;
import de.vsy.server.client_handling.data_management.bean.LocalClientDataProvider;
import de.vsy.server.persistent_data.server_data.temporal.LiveClientStateDAO;
import de.vsy.server.server.client_management.ClientState;
import de.vsy.server.server.client_management.ClientStateTranslator;
import de.vsy.server.server.data.access.HandlerAccessManager;
import de.vsy.shared_transmission.shared_transmission.packet.property.packet_category.PacketCategory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Set;

/**
 * Auf die Bedürfnisse des Klienten/Handlers zugeschnitten. Translator wird genutzt.
 */
public
class ClientSubscriptionHandler implements ClientStateListener {

    private static final Logger LOGGER = LogManager.getLogger();
    private static LiveClientStateDAO persistentClientStates;
    private static AbstractPacketCategorySubscriptionManager subscriptionHandler;
    private final ExtraClientSubscriptionProvider extraSubscriptionProvider;
    private final LocalClientDataProvider localClientDataProvider;
    private final ThreadPacketBufferManager clientBuffer;

    public
    ClientSubscriptionHandler (
            final ExtraClientSubscriptionProvider extraSubscriptionProvider,
            final LocalClientDataProvider localClientDataManager,
            final ThreadPacketBufferManager clientBuffer) {

        this.extraSubscriptionProvider = extraSubscriptionProvider;
        this.localClientDataProvider = localClientDataManager;
        this.clientBuffer = clientBuffer;
    }

    public static
    void setupStaticServerDataAccess () {
        subscriptionHandler = HandlerAccessManager.getClientSubscriptionManager();
        persistentClientStates = HandlerAccessManager.getClientStateAccessManager();
    }

    @Override
    public
    void evaluateNewState (final ClientState clientState, final boolean changeTo) {
        final SubscriptionHandler subscriptionLogic;
        final ExtraSubscriptionHandler extraSubscriptionLogic;
        final var clientId = this.localClientDataProvider.getClientId();
        final var threadIdMap = ClientStateTranslator.prepareClientSubscriptionMap(
                clientState, changeTo, clientId);
        final var extraSubscriptionMap = this.extraSubscriptionProvider.getExtraSubscriptionsForState(
                clientState);

        if (changeTo) {
            subscriptionLogic = subscriptionHandler::subscribe;
            extraSubscriptionLogic = persistentClientStates::addExtraSubscription;
        } else {
            subscriptionLogic = subscriptionHandler::unsubscribe;
            extraSubscriptionLogic = persistentClientStates::removeExtraSubscription;
        }

        if (!handleSubscribing(threadIdMap, subscriptionLogic)) {
            LOGGER.error("Subscription Error -> Exception sinnvoll");
        }

        if (!extraSubscriptionMap.isEmpty()) {
            threadIdMap.putAll(extraSubscriptionMap);

            if (!handleExtraSubscribing(clientId, extraSubscriptionMap,
                                        extraSubscriptionLogic)) {
                LOGGER.error("Extra-Subscription Error -> Exception sinnvoll");
            }
        }
    }

    private
    boolean handleSubscribing (final Map<PacketCategory, Set<Integer>> threadIdMap,
                               final SubscriptionHandler handler) {
        final var handlerBoundBuffer = this.clientBuffer.getPacketBuffer(
                ThreadPacketBufferLabel.HANDLER_BOUND);

        for (var topicEntry : threadIdMap.entrySet()) {
            final var topic = topicEntry.getKey();
            final var threadIdSet = topicEntry.getValue();

            for (var currentThreadId : threadIdSet) {
                final var stateChanged = handler.handle(topic, currentThreadId,
                                                        handlerBoundBuffer);

                if (!stateChanged) {
                    LOGGER.error("(De-)Abonnement fehlgeschlagen." +
                                 " Topic: {}; Thread: {}; Handler: {};" +
                                 " Buffer: {}", topic, currentThreadId,
                                 handler.getClass().getSimpleName(),
                                 handlerBoundBuffer);
                    return false;
                }
            }
        }
        return true;
    }

    private
    boolean handleExtraSubscribing (final int clientId,
                                    final Map<PacketCategory, Set<Integer>> extraSubscriptions,
                                    final ExtraSubscriptionHandler extraSubscriptionLogic) {

        for (final var topicSubscriptionSet : extraSubscriptions.entrySet()) {
            final var currentTopic = topicSubscriptionSet.getKey();

            for (final var currentThread : topicSubscriptionSet.getValue()) {

                if (!extraSubscriptionLogic.handle(clientId, currentTopic,
                                                   currentThread)) {
                    return false;
                }
            }
        }
        return true;
    }
}
