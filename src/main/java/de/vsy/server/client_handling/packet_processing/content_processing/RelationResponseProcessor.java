package de.vsy.server.client_handling.packet_processing.content_processing;

import de.vsy.server.client_handling.data_management.RelationHandlingDataProvider;
import de.vsy.server.persistent_data.client_data.ContactListDAO;
import de.vsy.server.persistent_data.client_data.MessageDAO;
import de.vsy.server.persistent_data.data_bean.ConvertCommDataToDTO;
import de.vsy.server.persistent_data.manipulation_utility.RelationManipulator;
import de.vsy.server.server_packet.packet_creation.ResultingPacketContentHandler;
import de.vsy.shared_module.packet_exception.PacketProcessingException;
import de.vsy.shared_module.packet_processing.ContentProcessor;
import de.vsy.shared_transmission.packet.content.PacketContent;
import de.vsy.shared_transmission.packet.content.relation.ContactRelationResponseDTO;
import de.vsy.shared_transmission.packet.content.relation.EligibleContactEntity;
import de.vsy.shared_transmission.packet.content.status.ContactStatusChangeDTO;
import de.vsy.shared_utility.id_manipulation.IdComparator;

import java.util.Collections;
import java.util.Set;

public class RelationResponseProcessor implements ContentProcessor<ContactRelationResponseDTO> {

    private final RelationHandlingDataProvider threadDataAccess;
    private final ResultingPacketContentHandler contentHandler;
    private final ContactListDAO contactListAccess;
    private final MessageDAO messageHistoryAccess;

    public RelationResponseProcessor(final RelationHandlingDataProvider threadDataAccess) {
        this.threadDataAccess = threadDataAccess;
        this.contentHandler = threadDataAccess.getResultingPacketContentHandler();
        this.contactListAccess = threadDataAccess.getLocalClientStateObserverManager()
                .getClientPersistentAccess().getContactListDAO();
        this.messageHistoryAccess = threadDataAccess.getLocalClientStateObserverManager()
                .getClientPersistentAccess().getMessageDAO();
    }

    @Override
    public void processContent(ContactRelationResponseDTO extractedContent)
            throws PacketProcessingException {
        final var contactId = this.extractContactId(extractedContent);

        checkResponseLegitimacy(extractedContent, contactId);
        final var iAmOriginator = this.checkClientOriginator(extractedContent);
        final var requestData = extractedContent.getRequestData();
        final var isFriendshipRequest = requestData.getDesiredState();

        if (isFriendshipRequest && extractedContent.getDecision()) {
            RelationManipulator.addContact(requestData.getContactType(), contactId,
                    this.contactListAccess);
        } else {
            if (iAmOriginator && !isFriendshipRequest) {
                RelationManipulator.removeContact(requestData.getContactType(), contactId,
                        this.contactListAccess,
                        this.messageHistoryAccess);
            }
        }
        this.contentHandler.addRequest(extractedContent);
    }

    private int extractContactId(final ContactRelationResponseDTO responseData) {
        final var requestData = responseData.getRequestData();
        final var originatorId = requestData.getOriginatorId();
        final var recipientId = requestData.getRecipientId();
        final var clientId = this.threadDataAccess.getLocalClientDataProvider().getClientId();

        return IdComparator.determineContactId(clientId, originatorId, recipientId);
    }

    /**
     * Check request legitimacy.
     *
     * @param contactResponse the contact request
     */
    private void checkResponseLegitimacy(final ContactRelationResponseDTO contactResponse,
                                         final int contactId)
            throws PacketProcessingException {
        final var requestData = contactResponse.getRequestData();
        final var recipientId = requestData.getRecipientId();
        final var desiredState = requestData.getDesiredState();
        final var contactsAlready = this.contactListAccess.checkAcquaintance(
                requestData.getContactType(), recipientId);

        if (desiredState && contactsAlready) {
            final var contactData = this.threadDataAccess.getContactToActiveClientMapper()
                    .getContactData(contactId);
            throw new PacketProcessingException(
                    "Friendship response was not processed. You already are friends with "
                            + contactData.getDisplayName());
        } else if (contactResponse.getRespondingClient().getCommunicatorId() != contactId
                && !desiredState) {
            throw new PacketProcessingException(
                    "Friendship response was not processed. Your response was already created automatically.");
        }
    }

    private boolean checkClientOriginator(final ContactRelationResponseDTO responseData) {
        final var clientId = this.threadDataAccess.getLocalClientDataProvider().getClientId();
        final var originatorId = responseData.getRequestData().getOriginatorId();
        return clientId == originatorId;
    }

    private boolean checkContactOnline(final int contactId) {
        return !(this.threadDataAccess.getContactToActiveClientMapper()
                .removeOfflineContacts(Set.of(contactId))
                .isEmpty());
    }
}
