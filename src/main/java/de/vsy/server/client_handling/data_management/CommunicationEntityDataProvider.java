
package de.vsy.server.client_handling.data_management;

import de.vsy.server.data.PacketCategorySubscriptionManager;
import de.vsy.server.data.access.CommunicatorDataManipulator;
import de.vsy.server.persistent_data.data_bean.CommunicatorData;
import de.vsy.server.persistent_data.data_bean.ConvertCommDataToDTO;
import de.vsy.shared_transmission.dto.CommunicatorDTO;

import java.util.HashSet;
import java.util.Set;

import static de.vsy.shared_transmission.packet.property.packet_category.PacketCategory.CHAT;

/**
 * Allows for limited access to contact data and contact states.
 */
public class CommunicationEntityDataProvider {

    private final PacketCategorySubscriptionManager activeContactLists;
    private final CommunicatorDataManipulator clientData;

    public CommunicationEntityDataProvider(
            final PacketCategorySubscriptionManager activeContactLists,
            final CommunicatorDataManipulator clientData) {
        this.activeContactLists = activeContactLists;
        this.clientData = clientData;
    }

    /**
     * Creates a set of CommunicatorDTO from a set of Integer ids.
     *
     * @param contactList Set<Integer> contact ids
     * @return Set<CommunicatorDTO>
     */
    public Set<CommunicatorDTO> mapToContactData(final Set<Integer> contactList) {
        final Set<CommunicatorDTO> activeContactList = new HashSet<>();

        contactList.forEach(contactId -> {
            var contactData = getContactData(contactId);
            var contactDTO = ConvertCommDataToDTO.convertFrom(contactData);
            activeContactList.add(contactDTO);
        });
        return activeContactList;
    }

    /**
     * Returns corresponding CommunicatorDTO for specified id.
     *
     * @param contactId int
     * @return CommunicatorDTO
     */
    public CommunicatorData getContactData(final int contactId) {
        return this.clientData.getCommunicatorData(contactId);
    }

    /**
     * Removes all offline contacts from a set of contact ids.
     *
     * @param activeClientIdList Set<Integer>
     * @return Set<Integer>
     */
    public Set<Integer> removeOfflineContacts(final Set<Integer> activeClientIdList) {
        return this.activeContactLists.checkThreadIds(CHAT, activeClientIdList);
    }
}
