package de.vsy.server.client_handling.data_management;

import static de.vsy.shared_transmission.shared_transmission.packet.property.packet_category.PacketCategory.CHAT;

import de.vsy.server.persistent_data.client_data.ContactListDAO;
import de.vsy.server.client_management.ClientState;
import de.vsy.shared_transmission.shared_transmission.packet.content.relation.EligibleContactEntity;
import de.vsy.shared_transmission.shared_transmission.packet.property.packet_category.PacketCategory;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ExtraClientSubscriptionProvider {

  private final ContactListDAO contactListAccessor;

  public ExtraClientSubscriptionProvider(final ContactListDAO contactListAccessor) {
    this.contactListAccessor = contactListAccessor;
  }

  public Map<PacketCategory, Set<Integer>> getExtraSubscriptionsForState(
      final ClientState clientState) {
    Map<PacketCategory, Set<Integer>> extraSubscriptions = new EnumMap<>(PacketCategory.class);
    Set<Integer> threadList;

    if (ClientState.ACTIVE_MESSENGER.equals(clientState)) {
      threadList = new HashSet<>(
          this.contactListAccessor.readContacts(EligibleContactEntity.GROUP));
      extraSubscriptions.put(CHAT, threadList);
    }

    return extraSubscriptions;
  }
}
