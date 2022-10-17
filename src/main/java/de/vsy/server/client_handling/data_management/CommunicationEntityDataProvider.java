/*
 *
 */
package de.vsy.server.client_handling.data_management;

import static de.vsy.shared_transmission.shared_transmission.packet.property.packet_category.PacketCategory.CHAT;

import de.vsy.server.persistent_data.data_bean.CommunicatorData;
import de.vsy.server.persistent_data.data_bean.ConvertCommDataToDTO;
import de.vsy.server.server.data.AbstractPacketCategorySubscriptionManager;
import de.vsy.server.server.data.access.CommunicatorDataManipulator;
import de.vsy.shared_transmission.shared_transmission.dto.CommunicatorDTO;
import java.util.HashSet;
import java.util.Set;

/**
 * Einheit zur Ausgabe von Daten aktiver Freunde. Frederic Heath
 */
public class CommunicationEntityDataProvider {

  private final AbstractPacketCategorySubscriptionManager activeContactLists;
  private final CommunicatorDataManipulator clientData;

  /**
   * Liefert ein Objekt, das Zugriff auf die Freundesliste des Clients gewährt.
   *
   * @param activeContactLists the active contact lists
   * @param clientData         the client dataManagement
   */
  public CommunicationEntityDataProvider(
      final AbstractPacketCategorySubscriptionManager activeContactLists,
      final CommunicatorDataManipulator clientData) {
    this.activeContactLists = activeContactLists;
    this.clientData = clientData;
  }

  /**
   * Erstellt aus einer Liste von intn eine Liste von Clientdaten.
   *
   * @param contactList Liste von intn, zu denen Clientdaten benötigt werden.
   * @return Liste von Clientdaten, vom Typ Arraylist
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
   * Gibt Clientdaten eines Freundes (bestimmt durch die Identifikationsnummer) aus.
   *
   * @param contactId the contact id
   * @return Clientdaten vom Typ Contact, falls gültige Id und befreundet.
   */
  public CommunicatorData getContactData(final int contactId) {
    return this.clientData.getCommunicatorData(contactId);
  }

  /**
   * Entfernt alle Clientdaten aus einer Liste, die nicht in der Freundesliste enthalten sind.
   *
   * @param activeClientIdList the contactList
   * @return Die bereinigte List von Clientdaten
   */
  public Set<Integer> removeOfflineContacts(final Set<Integer> activeClientIdList) {
    return this.activeContactLists.checkThreadIds(CHAT, activeClientIdList);
  }
}
