package de.vsy.server.data;

import de.vsy.server.client_handling.data_management.ResponseRetainer;
import de.vsy.server.persistent_data.client_data.PendingPacketDAO;
import de.vsy.server.service.RemotePacketBuffer;
import de.vsy.server.service.request.CategoryIdSubscriber;
import de.vsy.shared_module.data_element_validation.IdCheck;
import de.vsy.shared_module.packet_exception.PacketTransmissionException;
import de.vsy.shared_module.packet_management.PacketBuffer;
import de.vsy.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.packet.property.packet_category.PacketCategory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ClientSubscriptionManager extends PacketCategorySubscriptionManager {

    private final Set<Integer> remoteClients;

    public ClientSubscriptionManager() {
        this(new ConcurrentHashMap<>());
    }

    public ClientSubscriptionManager(
            final Map<PacketCategory, Map<Integer, CategoryIdSubscriber>> subscriptions) {
        super(evaluateExistingSubscriptions(subscriptions));
        this.remoteClients = new HashSet<>();
    }

    private static Map<PacketCategory, Map<Integer, CategoryIdSubscriber>> evaluateExistingSubscriptions(
            final Map<PacketCategory, Map<Integer, CategoryIdSubscriber>> subscriptions) {
        if (subscriptions != null) {

            for (var topicSubs : subscriptions.entrySet()) {
                var threadSubscriptions = topicSubs.getValue();

                for (var threadSubscription : threadSubscriptions.entrySet()) {
                    int threadId = threadSubscription.getKey();
                    var subscriber = threadSubscription.getValue();
                    var idCheckString = IdCheck.checkData(threadId);

                    if (idCheckString.isPresent() && subscriber != null) {
                        continue;
                    }
                    threadSubscriptions.remove(threadId);
                }
            }
        }
        return subscriptions;
    }

    @Override
    public void publish(Packet packetToPublish) throws PacketTransmissionException {
        CategoryIdSubscriber subscriptionBuffers;
        Map<Integer, CategoryIdSubscriber> topicSubscriptions;
        final var packetProperties = packetToPublish.getPacketProperties();

        topicSubscriptions = super.getTopicSubscriptions(
                packetProperties.getPacketIdentificationProvider().getPacketCategory());

        subscriptionBuffers = topicSubscriptions.get(packetProperties.getRecipient().getEntityId());

        if (subscriptionBuffers != null) {
            subscriptionBuffers.publish(packetToPublish);
        } else {
            final var packet = ResponseRetainer.retainIfResponse(packetToPublish);

            if(packet != null) {
                throw new PacketTransmissionException("Packet could not be delivered. Contact offline.");
            }
        }
    }

    @Override
    public boolean subscribe(final PacketCategory topic, final int threadId,
                             final PacketBuffer bufferSuggestion) {
        if (bufferSuggestion instanceof RemotePacketBuffer) {
            this.remoteClients.add(threadId);
        }
        return super.subscribe(topic, threadId, bufferSuggestion);
    }

    @Override
    public boolean unsubscribe(PacketCategory topic, int threadId, PacketBuffer bufferSuggestion) {
        this.remoteClients.remove(threadId);
        return super.unsubscribe(topic, threadId, bufferSuggestion);
    }

    public Set<Integer> getLocalThreads(PacketCategory topic, Set<Integer> idsToCheck) {
        Set<Integer> validatedThreadIds = super.checkThreadIds(topic, idsToCheck);
        validatedThreadIds.removeAll(this.remoteClients);
        return validatedThreadIds;
    }
}
