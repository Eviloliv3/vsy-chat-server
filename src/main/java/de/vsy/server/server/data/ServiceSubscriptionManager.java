package de.vsy.server.server.data;

import de.vsy.server.service.request.CategoryIdSubscriber;
import de.vsy.shared_module.shared_module.packet_exception.PacketTransmissionException;
import de.vsy.shared_transmission.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.shared_transmission.packet.property.packet_category.PacketCategory;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public
class ServiceSubscriptionManager extends AbstractPacketCategorySubscriptionManager {

    private static final ThreadLocalRandom RANDOM_NUMBER_GENERATOR;

    static {
        RANDOM_NUMBER_GENERATOR = ThreadLocalRandom.current();
    }

    public
    ServiceSubscriptionManager () {
        this(new ConcurrentHashMap<>());
    }

    public
    ServiceSubscriptionManager (
            final Map<PacketCategory, Map<Integer, CategoryIdSubscriber>> subscriptions) {
        super(subscriptions);
    }

    @Override
    public
    void publish (Packet publishedPacket)
    throws PacketTransmissionException {
        CategoryIdSubscriber subscriptionBuffers;
        Map<Integer, CategoryIdSubscriber> topicSubscriptions;
        var packetProperties = publishedPacket.getPacketProperties();

        topicSubscriptions = super.getTopicSubscriptions(
                packetProperties.getPacketIdentificationProvider()
                                .getPacketCategory());

        subscriptionBuffers = getRandomServiceSubscription(topicSubscriptions);

        if (subscriptionBuffers != null) {
            subscriptionBuffers.publish(publishedPacket);
        } else {

            throw new PacketTransmissionException(
                    "Paket wurde nicht " + "zugestellt. Kein " +
                    "Service hinterlegt.");
        }
    }

    @Override
    public
    Set<Integer> getLocalThreads (PacketCategory topic, Set<Integer> idsToCheck) {
        return super.checkThreadIds(topic, idsToCheck);
    }

    private
    CategoryIdSubscriber getRandomServiceSubscription (
            Map<Integer, CategoryIdSubscriber> topicSubscriptions) {
        CategoryIdSubscriber chosenServiceSubscription = null;

        while (chosenServiceSubscription == null) {
            var allCategorySubscriptions = new ArrayList<>(
                    topicSubscriptions.entrySet());

            if (!allCategorySubscriptions.isEmpty()) {
                var randomSubscription = RANDOM_NUMBER_GENERATOR.nextInt(
                        allCategorySubscriptions.size());
                var randomServiceSubscription = allCategorySubscriptions.get(
                        randomSubscription);

                if (randomServiceSubscription != null) {
                    chosenServiceSubscription = randomServiceSubscription.getValue();

                    if (chosenServiceSubscription == null) {
                        topicSubscriptions.remove(
                                randomServiceSubscription.getKey());
                    }
                }
            } else {
                break;
            }
        }
        return chosenServiceSubscription;
    }
}
