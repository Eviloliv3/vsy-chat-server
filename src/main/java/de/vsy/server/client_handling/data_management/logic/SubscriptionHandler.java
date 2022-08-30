package de.vsy.server.client_handling.data_management.logic;

import de.vsy.shared_module.shared_module.packet_management.PacketBuffer;
import de.vsy.shared_transmission.shared_transmission.packet.property.packet_category.PacketCategory;

public
interface SubscriptionHandler {

    boolean handle (final PacketCategory topic, final int topicId,
                    final PacketBuffer subscriptionBuffer);
}
