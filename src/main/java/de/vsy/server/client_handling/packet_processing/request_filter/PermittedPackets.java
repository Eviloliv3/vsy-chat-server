package de.vsy.server.client_handling.packet_processing.request_filter;

import de.vsy.shared_transmission.packet.property.packet_category.PacketCategory;

import java.util.ArrayList;
import java.util.List;

/**
 * The Class PermittedPackets.
 */
public class PermittedPackets extends PermittedCategoryProvider {

    private final List<PacketCategory> categories;

    /**
     * Instantiates a new permitted Packet.
     *
     * @param categories the categories
     */
    public PermittedPackets(final List<PacketCategory> categories) {
        this(categories, null);
    }

    /**
     * Instantiates a new permitted Packet.
     *
     * @param categories      the categories
     * @param otherCategories the other categories
     */
    public PermittedPackets(final List<PacketCategory> categories,
                            final PermittedCategoryProvider otherCategories) {
        super(otherCategories);
        this.categories = categories;
    }

    @Override
    public List<PacketCategory> getPermittedPacketCategories() {
        final List<PacketCategory> permittedCategories = new ArrayList<>();

        if (super.otherPermittedCategories != null) {
            permittedCategories.addAll(super.otherPermittedCategories.getPermittedPacketCategories());
        }
        permittedCategories.addAll(this.categories);

        return permittedCategories;
    }
}
