package de.vsy.chat.server.server.data;

import de.vsy.chat.shared_module.data_element_validation.IdCheck;
import de.vsy.chat.shared_module.packet_exception.PacketTransmissionException;
import de.vsy.chat.shared_module.packet_management.PacketBuffer;
import de.vsy.chat.server.service.RemotePacketBuffer;
import de.vsy.chat.server.service.request.CategoryIdSubscriber;
import de.vsy.chat.shared_transmission.packet.Packet;
import de.vsy.chat.shared_transmission.packet.property.packet_category.PacketCategory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public
class ClientSubscriptionManager extends AbstractPacketCategorySubscriptionManager {

    private final Set<Integer> remoteClients;

    public
    ClientSubscriptionManager () {
        this(new ConcurrentHashMap<>());
    }

    public
    ClientSubscriptionManager (
            final Map<PacketCategory, Map<Integer, CategoryIdSubscriber>> subscriptions) {
        super(evaluateExistingSubscriptions(subscriptions));
        this.remoteClients = new HashSet<>();
    }

    private static
    Map<PacketCategory, Map<Integer, CategoryIdSubscriber>> evaluateExistingSubscriptions (
            final Map<PacketCategory, Map<Integer, CategoryIdSubscriber>> subscriptions) {
        if (subscriptions != null) {

            for (var topicSubs : subscriptions.entrySet()) {
                var threadSubscriptions = topicSubs.getValue();

                for (var threadSubscription : threadSubscriptions.entrySet()) {
                    int threadId = threadSubscription.getKey();
                    var subscriber = threadSubscription.getValue();
                    var idCheckString = IdCheck.checkData(threadId);

                    if (idCheckString == null && subscriber != null) {
                        continue;
                    }
                    threadSubscriptions.remove(threadId);
                }
            }
        }
        return subscriptions;
    }

    @Override
    public
    void publish (Packet packetToPublish)
    throws PacketTransmissionException {
        CategoryIdSubscriber subscriptionBuffers;
        Map<Integer, CategoryIdSubscriber> topicSubscriptions;
        final var packetProperties = packetToPublish.getPacketProperties();

        topicSubscriptions = super.getTopicSubscriptions(
                packetProperties.getContentIdentifier().getPacketCategory());

        subscriptionBuffers = topicSubscriptions.get(
                packetProperties.getRecipientEntity().getEntityId());

        if (subscriptionBuffers != null) {
            final var notReceived = subscriptionBuffers.publish(packetToPublish);

            if (notReceived > 0) {
                LOGGER.info("{} wurde(n) nicht informiert.", notReceived);
            }
        } else {
            throw new PacketTransmissionException(
                    "Paket wurde nicht zugestellt. Kontakt offline.");
        }
    }

    @Override
    public
    boolean subscribe (final PacketCategory topic, final int threadId,
                       final PacketBuffer bufferSuggestion) {
        if (bufferSuggestion instanceof RemotePacketBuffer) {
            this.remoteClients.add(threadId);
        }
        return super.subscribe(topic, threadId, bufferSuggestion);
    }

    @Override
    public
    boolean unsubscribe (PacketCategory topic, int threadId,
                         PacketBuffer bufferSuggestion) {
        this.remoteClients.remove(threadId);
        return super.unsubscribe(topic, threadId, bufferSuggestion);
    }

    public
    Set<Integer> validateThreadIds (PacketCategory topic, Set<Integer> idsToCheck) {
        Set<Integer> validatedThreadIds = super.checkThreadIds(topic, idsToCheck);
        validatedThreadIds.removeAll(this.remoteClients);
        return validatedThreadIds;
    }
}
