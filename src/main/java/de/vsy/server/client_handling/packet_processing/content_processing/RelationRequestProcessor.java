package de.vsy.server.client_handling.packet_processing.content_processing;

import de.vsy.server.client_handling.data_management.CommunicationEntityDataProvider;
import de.vsy.server.client_handling.data_management.RelationHandlingDataProvider;
import de.vsy.server.persistent_data.client_data.ContactListDAO;
import de.vsy.server.server_packet.packet_creation.ResultingPacketContentHandler;
import de.vsy.shared_module.packet_exception.PacketProcessingException;
import de.vsy.shared_module.packet_management.ClientDataProvider;
import de.vsy.shared_module.packet_processing.ContentProcessor;
import de.vsy.shared_transmission.packet.content.relation.ContactRelationRequestDTO;
import de.vsy.shared_utility.id_manipulation.IdComparator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * PacketProcessor for contact relationship request type Packet.
 */
public class RelationRequestProcessor implements ContentProcessor<ContactRelationRequestDTO> {

    private static final Logger LOGGER = LogManager.getLogger();
    private final CommunicationEntityDataProvider contactMapper;
    private final ContactListDAO contactProvider;
    private final ClientDataProvider localClientDataManager;
    private final ResultingPacketContentHandler contentHandler;

    /**
     * Instantiates a new contact status change PacketHandler.
     *
     * @param threadDataAccess the thread dataManagement accessLimiter
     */
    public RelationRequestProcessor(final RelationHandlingDataProvider threadDataAccess) {

        this.contactMapper = threadDataAccess.getContactToActiveClientMapper();
        this.contactProvider = threadDataAccess.getLocalClientStateObserverManager()
                .getClientPersistentAccess()
                .getContactListDAO();
        this.localClientDataManager = threadDataAccess.getLocalClientDataProvider();
        this.contentHandler = threadDataAccess.getResultingPacketContentHandler();
    }

    @Override
    public void processContent(final ContactRelationRequestDTO extractedContent)
            throws PacketProcessingException {
        final var clientId = this.localClientDataManager.getClientId();
        final var isFriendshipRequest = extractedContent.getDesiredState();
        final var originatorId = extractedContent.getOriginatorId();
        final var recipientId = extractedContent.getRecipientId();
        final var iAmOriginator = clientId == originatorId;
        final var contactId = IdComparator.determineContactId(clientId, originatorId, recipientId);
        checkRequestLegitimacy(extractedContent, contactId);

        if (!iAmOriginator && !isFriendshipRequest) {
            this.contactProvider.removeContactFromSet(extractedContent.getContactType(), contactId);
            this.contentHandler.addResponse(
                    extractedContent.setDecision(this.localClientDataManager.getCommunicatorData(), false));
        }
        this.contentHandler.addRequest(extractedContent);
    }

    /**
     * Check request legitimacy.
     *
     * @param contactRequest the contact request
     */
    private void checkRequestLegitimacy(final ContactRelationRequestDTO contactRequest,
                                        final int contactId)
            throws PacketProcessingException {
        final var desiredState = contactRequest.getDesiredState();
        final var contactsAlready = this.contactProvider.checkAcquaintance(
                contactRequest.getContactType(), contactId);

        if (desiredState && contactsAlready) {
            final var contactData = this.contactMapper.getContactData(contactId);
            throw new PacketProcessingException(
                    "Friendship request was not processed. You already are friends with "
                            + contactData.getDisplayName());
        } else {
            if (!desiredState && !contactsAlready) {
                throw new PacketProcessingException(
                        "Friendship request was not processed. " + contactId + " is no contact of yours.");
            }
        }
    }
}
