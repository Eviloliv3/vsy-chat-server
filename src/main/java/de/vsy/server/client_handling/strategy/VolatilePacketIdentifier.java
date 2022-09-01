package de.vsy.server.client_handling.strategy;

import de.vsy.shared_transmission.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.shared_transmission.packet.property.packet_category.PacketCategory;

import java.util.Set;

public
class VolatilePacketIdentifier {

    private static final Set<PacketCategory> volatileCategories = Set.of(
            PacketCategory.AUTHENTICATION, PacketCategory.STATUS);

    private
    VolatilePacketIdentifier () {}

    public static
    boolean checkPacketVolatiliy (final Packet toCheck) {
        return volatileCategories.contains(toCheck.getPacketProperties()
                                                  .getPacketIdentificationProvider()
                                                  .getPacketCategory());
    }
}
