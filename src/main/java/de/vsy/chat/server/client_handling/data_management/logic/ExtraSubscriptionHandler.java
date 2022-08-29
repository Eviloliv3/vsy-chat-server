package de.vsy.chat.server.client_handling.data_management.logic;

import de.vsy.chat.shared_transmission.packet.property.packet_category.PacketCategory;

public
interface ExtraSubscriptionHandler {

    boolean handle (final int clientId, final PacketCategory topic,
                    final int threadId);
}
