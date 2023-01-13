package de.vsy.server.client_handling.data_management;

import de.vsy.server.persistent_data.client_data.ContactListDAO;
import de.vsy.shared_transmission.packet.content.relation.EligibleContactEntity;
import de.vsy.shared_transmission.packet.property.packet_category.PacketCategory;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static de.vsy.shared_transmission.packet.property.packet_category.PacketCategory.CHAT;

public class ExtraClientSubscriptionProvider {


    protected ExtraClientSubscriptionProvider() {
    }

    public static Map<PacketCategory, Set<Integer>> createGroupSubscriptions(final ContactListDAO contactListAccessor) {
        final var extraSubscriptions = new EnumMap<PacketCategory, Set<Integer>>(PacketCategory.class);
        var groups = contactListAccessor.readContacts(EligibleContactEntity.GROUP);
        var groupCopy = new HashSet<>(groups);
        extraSubscriptions.put(CHAT, groupCopy);
        return extraSubscriptions;
    }
}
