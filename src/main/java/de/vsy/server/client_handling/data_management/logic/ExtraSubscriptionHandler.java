package de.vsy.server.client_handling.data_management.logic;

import de.vsy.shared_transmission.shared_transmission.packet.property.packet_category.PacketCategory;

public
interface ExtraSubscriptionHandler {

    boolean handle (final int clientId, final PacketCategory topic,
                    final int threadId);
}
