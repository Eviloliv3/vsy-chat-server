package de.vsy.server.client_handling.packet_processing.content_processing;

import de.vsy.server.client_handling.data_management.access_limiter.StatusHandlingDataProvider;
import de.vsy.server.client_handling.data_management.logic.ClientStateControl;
import de.vsy.server.client_management.ClientState;
import de.vsy.server.persistent_data.client_data.ContactListDAO;
import de.vsy.server.persistent_data.client_data.MessageDAO;
import de.vsy.server.server_packet.packet_creation.ResultingPacketContentHandler;
import de.vsy.shared_module.packet_processing.ContentProcessor;
import de.vsy.shared_transmission.packet.content.status.ContactStatusChangeDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static de.vsy.server.client_management.ClientState.ACTIVE_MESSENGER;
import static de.vsy.shared_transmission.packet.content.status.ClientService.MESSENGER;

/**
 * The Class StatusChangeProcessor.
 */
public class ContactStatusChangeProcessor implements ContentProcessor<ContactStatusChangeDTO> {

    private static final Logger LOGGER = LogManager.getLogger();
    private final ContactListDAO contactProvider;
    private final MessageDAO messageReader;
    private final ResultingPacketContentHandler contentHandler;

    /**
     * Instantiates a new status change handler.
     *
     * @param threadDataAccess the thread dataManagement accessLimiter
     */
    public ContactStatusChangeProcessor(final StatusHandlingDataProvider threadDataAccess) {
        this.contactProvider = threadDataAccess.getLocalClientStateDependentLogicProvider()
                .getClientPersistentAccess()
                .getContactListDAO();
        this.messageReader = threadDataAccess.getLocalClientStateDependentLogicProvider()
                .getClientPersistentAccess()
                .getMessageDAO();
        contentHandler = threadDataAccess.getResultingPacketContentHandler();
    }

    @Override
    public void processContent(ContactStatusChangeDTO extractedContent) {
        var contactId = extractedContent.getContactData().getCommunicatorId();

        if (this.contactProvider.checkAcquaintance(contactId)) {

            if (extractedContent.getServiceType().equals(MESSENGER) &&
                    extractedContent.getOnlineStatus()) {
                LOGGER.info("Contact {} online.", contactId);
                final var messageHistory = this.messageReader.readClientMessages(contactId);
                this.contentHandler
                        .addRequest(
                                ContactStatusChangeDTO.addMessageHistory(extractedContent, messageHistory));
            } else {
                LOGGER.info("Contact {} offline.", contactId);
                this.contentHandler.addRequest(extractedContent);
            }
        } else {
            LOGGER.info("Status notification will be dropped. Unknown contact.");
        }
    }
}
