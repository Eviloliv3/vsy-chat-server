package de.vsy.chat.server.client_handling.packet_processing.request_filter;

import de.vsy.chat.shared_transmission.packet.property.packet_category.PacketCategory;

import java.util.List;

import static de.vsy.chat.shared_transmission.packet.property.packet_category.PacketCategory.RELATION;
import static de.vsy.chat.shared_transmission.packet.property.packet_category.PacketCategory.STATUS;
import static java.util.List.of;

/** The Class AuthenticationPermittedPackets. */
public
class AuthenticationPermittedPackets extends PermittedPackets {

    /** Instantiates a new authentication permitted Packet. */
    public
    AuthenticationPermittedPackets () {
        this(null);
    }

    /**
     * Instantiates a new authentication permitted Packet.
     *
     * @param otherCategories the other categories
     */
    public
    AuthenticationPermittedPackets (
            final PermittedCategoryProvider otherCategories) {
        super(getPermittedCategories(), otherCategories);
    }

    /**
     * Gets the permitted categories.
     *
     * @return the permitted categories
     */
    private static
    List<PacketCategory> getPermittedCategories () {
        return of(STATUS, RELATION);
    }
}
