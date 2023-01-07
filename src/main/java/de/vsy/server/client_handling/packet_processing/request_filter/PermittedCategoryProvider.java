package de.vsy.server.client_handling.packet_processing.request_filter;

import de.vsy.shared_transmission.packet.property.packet_category.PacketCategory;

import java.util.List;

/**
 * The Class PermittedCategoryProvider.
 */
public abstract class PermittedCategoryProvider {

    protected final PermittedCategoryProvider otherPermittedCategories;

    protected PermittedCategoryProvider(final PermittedCategoryProvider otherCategories) {
        this.otherPermittedCategories = otherCategories;
    }

    /**
     * Returns the permitted PacketCategories.
     *
     * @return List<PacketCategory>
     */
    public abstract List<PacketCategory> getPermittedPacketCategories();
}
