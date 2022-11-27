/*
 *
 */
package de.vsy.server.client_handling.packet_processing.content_processing;

import de.vsy.server.client_handling.data_management.access_limiter.ChatHandlingDataProvider;
import de.vsy.server.persistent_data.client_data.ContactListDAO;
import de.vsy.server.persistent_data.client_data.MessageDAO;
import de.vsy.server.server_packet.packet_creation.ResultingPacketContentHandler;
import de.vsy.shared_module.packet_exception.PacketProcessingException;
import de.vsy.shared_module.packet_management.ClientDataProvider;
import de.vsy.shared_module.packet_processing.ContentProcessor;
import de.vsy.shared_transmission.packet.content.chat.TextMessageDTO;
import de.vsy.shared_utility.id_manipulation.IdComparator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TextMessageProcessor implements ContentProcessor<TextMessageDTO> {

  private static final Logger LOGGER = LogManager.getLogger();
  private final ContactListDAO contactListAccess;
  private final MessageDAO messageWriter;
  private final ResultingPacketContentHandler resultingPacketCache;
  private final ClientDataProvider localClientData;

  /**
   * Instantiates a new message PacketHandler.
   *
   * @param threadDataAccess the thread dataManagement accessLimiter
   */
  public TextMessageProcessor(final ChatHandlingDataProvider threadDataAccess) {
    this.contactListAccess = threadDataAccess.getLocalClientStateDependentLogicProvider()
        .getClientPersistentAccess().getContactlistDAO();
    this.messageWriter = threadDataAccess.getLocalClientStateDependentLogicProvider()
        .getClientPersistentAccess()
        .getMessageDAO();
    this.localClientData = threadDataAccess.getLocalClientDataProvider();
    this.resultingPacketCache = threadDataAccess.getResultingPacketContentHandler();
  }

  @Override
  public void processContent(TextMessageDTO extractedContent) throws PacketProcessingException {
    // Ein Klient koennte theoretisch einen Kontakt ohne dessen Wissen zuspamen.
    // Die empfangenen Nahrichten wuerden erst nach der naechsten Kontaktstatus-
    // aktualisierung einseitig angezeigt werden. Verhindert wuerde dieses Server-
    // verhalten durch eine eingehendere Pruefung der Pakete durch den Simple-
    // PacketChecker -> TextMessageDTO.originatorId == Properties.senderId muss
    // dann einen gesetzten Empfangsstatus ausschließen !! An anderer Stelle
    // ist diese Schwachstelle nicht zu schließen.
    checkLegitimacy(extractedContent);

    final var clientId = this.localClientData.getClientId();
    final var originatorId = extractedContent.getOriginatorId();
    final var receptionState = extractedContent.getReceptionState();
    final var iAmOriginator = IdComparator.determineIfOriginator(clientId, originatorId);
    final var messageToReturn = !iAmOriginator && !receptionState;
    final var returnedMessage = iAmOriginator && receptionState;

    if (messageToReturn || returnedMessage) {
      if (!saveMessage(extractedContent)) {
        LOGGER.error("Error occurred while saving test message.\n{}", extractedContent);
      }
    }

    if (!receptionState || iAmOriginator) {
      this.resultingPacketCache.addRequest(extractedContent);
    }

    if (messageToReturn) {
      this.resultingPacketCache.addResponse(extractedContent.setReceptionState());
    }
  }

  private void checkLegitimacy(final TextMessageDTO textMessage) throws PacketProcessingException {
    final var contactId = IdComparator.determineContactId(localClientData.getClientId(),
        textMessage.getOriginatorId(), textMessage.getRecipientId());
    final var contactType = textMessage.getContactType();

    if (!(this.contactListAccess.checkContact(contactType, contactId))) {
      throw new PacketProcessingException(
          "Textnachricht wurde nicht " + "zugestellt. " + contactId
              + " ist kein Kontakt von Ihnen.");
    }
  }

  /**
   * Save message.
   *
   * @param messageData the message dataManagement
   * @return true, if successful
   */
  private boolean saveMessage(final TextMessageDTO messageData) {
    final var recipientId = messageData.getRecipientId();

    final var contactId =
        recipientId == this.localClientData.getClientId() ? messageData.getOriginatorId()
            : recipientId;
    return this.messageWriter.saveMessage(contactId, messageData);
  }
}
