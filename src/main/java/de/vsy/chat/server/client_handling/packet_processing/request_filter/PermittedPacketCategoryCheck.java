/*
 *
 */
package de.vsy.chat.server.client_handling.packet_processing.request_filter;

import de.vsy.chat.server.client_handling.data_management.bean.ClientStateListener;
import de.vsy.chat.server.server.client_management.ClientState;
import de.vsy.chat.shared_transmission.packet.property.packet_category.PacketCategory;

import java.util.*;

import static java.util.List.copyOf;

/**
 * Provides simple check for permitted client request Packet for each possible
 * ClientStates.
 */
public
class PermittedPacketCategoryCheck implements ClientStateListener {

    private final Map<ClientState, PermittedCategoryProvider> packetCategoriesPerClientState;
    private final Set<PacketCategory> permittedPackets;

    /** Instantiates a new permitted PacketCategory check. */
    public
    PermittedPacketCategoryCheck () {
        this.packetCategoriesPerClientState = new EnumMap<>(ClientState.class);
        this.permittedPackets = new HashSet<>();
        registerCategories();
    }

    private
    void registerCategories () {
        this.permittedPackets.addAll(
                new ConnectionPermittedPackets().getPermittedPacketCategories());

        this.packetCategoriesPerClientState.put(ClientState.AUTHENTICATED,
                                                new AuthenticationPermittedPackets());
        this.packetCategoriesPerClientState.put(ClientState.ACTIVE_MESSENGER,
                                                new ActiveMessengerPermittedPackets());
    }

    /**
     * Check PacketCategory.
     *
     * @param category the category
     *
     * @return true, if successful
     */
    public
    boolean checkPacketCategory (final PacketCategory category) {
        return getPermittedPacketCategories().contains(category);
    }

    /**
     * Gets the permitted Packetcategories.
     *
     * @return the permitted Packetcategories
     */
    public
    List<PacketCategory> getPermittedPacketCategories () {
        return copyOf(this.permittedPackets);
    }

    @Override
    public
    void evaluateNewState (ClientState changedState, boolean added) {
        var categoriesToChange = this.packetCategoriesPerClientState.get(
                changedState);

        if (categoriesToChange != null) {

            if (added) {
                this.permittedPackets.addAll(
                        categoriesToChange.getPermittedPacketCategories());
            } else {
                for (final var packetCategory : categoriesToChange.getPermittedPacketCategories()) {
                    this.permittedPackets.remove(packetCategory);
                }
            }
        }
    }
}
