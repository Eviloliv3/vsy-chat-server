package de.vsy.server.client_handling.packet_processing.content_processing;

import de.vsy.server.client_handling.data_management.access_limiter.RelationHandlingDataProvider;
import de.vsy.server.persistent_data.client_data.ContactListDAO;
import de.vsy.server.persistent_data.client_data.MessageDAO;
import de.vsy.server.persistent_data.data_bean.ConvertCommDataToDTO;
import de.vsy.server.persistent_data.manipulation_utility.RelationManipulator;
import de.vsy.server.server_packet.packet_creation.ResultingPacketContentHandler;
import de.vsy.shared_module.shared_module.packet_exception.PacketProcessingException;
import de.vsy.shared_module.shared_module.packet_processing.ContentProcessor;
import de.vsy.shared_transmission.shared_transmission.packet.content.PacketContent;
import de.vsy.shared_transmission.shared_transmission.packet.content.relation.ContactRelationResponseDTO;
import de.vsy.shared_transmission.shared_transmission.packet.content.relation.EligibleContactEntity;
import de.vsy.shared_transmission.shared_transmission.packet.content.status.ContactMessengerStatusDTO;
import de.vsy.shared_utility.id_manipulation.IdComparator;

import java.util.Collections;
import java.util.Set;

public
class RelationResponseProcessor
        implements ContentProcessor<ContactRelationResponseDTO> {

    private final RelationHandlingDataProvider threadDataAccess;
    private final ResultingPacketContentHandler contentHandler;
    private final ContactListDAO contactListAccess;
    private final MessageDAO messageHistoryAccess;

    public
    RelationResponseProcessor (final RelationHandlingDataProvider threadDataAccess) {
        this.threadDataAccess = threadDataAccess;
        this.contentHandler = threadDataAccess.getResultingPacketContentHandler();
        this.contactListAccess = threadDataAccess.getLocalClientStateDependentLogicProvider()
                                                 .getClientPersistentAccess()
                                                 .getContactlistDAO();
        this.messageHistoryAccess = threadDataAccess.getLocalClientStateDependentLogicProvider()
                                                    .getClientPersistentAccess()
                                                    .getMessageDAO();
    }

    @Override
    public
    void processContent (ContactRelationResponseDTO extractedContent)
    throws PacketProcessingException {
        final var contactId = this.extractContactId(extractedContent);

        checkResponseLegitimacy(extractedContent, contactId);
        final var requestData = extractedContent.getRequestData();
        final var isFriendshipRequest = requestData.getDesiredState();
        final var iAmOriginator = this.checkClientOriginator(extractedContent);

        if (isFriendshipRequest && extractedContent.getDecision()) {
            RelationManipulator.addContact(requestData.getContactType(),
                                           contactId, this.contactListAccess);
            this.appendStatusMessage(contactId, true);
        } else if (iAmOriginator && !isFriendshipRequest) {
            RelationManipulator.removeContact(requestData.getContactType(),
                                              contactId, this.contactListAccess,
                                              this.messageHistoryAccess);
            this.appendStatusMessage(contactId, false);
        }
        this.contentHandler.addResponse(extractedContent);
    }

    private
    int extractContactId (final ContactRelationResponseDTO responseData) {
        final var requestData = responseData.getRequestData();
        final var originatorId = requestData.getOriginatorId();
        final var recipientId = requestData.getRecipientId();
        final var clientId = this.threadDataAccess.getLocalClientDataProvider()
                                                  .getClientId();

        return IdComparator.determineContactId(clientId, originatorId, recipientId);
    }

    /**
     * Check request legitimacy.
     *
     * @param contactResponse the contact request
     */
    private
    void checkResponseLegitimacy (final ContactRelationResponseDTO contactResponse,
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
            throw new PacketProcessingException("Freundschaftsanfrage wurde nicht " +
                                                "verarbeitet. Sie sind bereits " +
                                                "mit " +
                                                contactData.getDisplayName() +
                                                " befreundet.");
        }
    }

    private
    boolean checkClientOriginator (final ContactRelationResponseDTO responseData) {
        final var clientId = this.threadDataAccess.getLocalClientDataProvider()
                                                  .getClientId();
        final var originatorId = responseData.getRequestData().getOriginatorId();
        return clientId == originatorId;
    }

    private
    void appendStatusMessage (final int contactId, boolean contactAdded) {
        final boolean contactToAdd;
        final PacketContent contactStatusContent;
        final var contactData = threadDataAccess.getContactToActiveClientMapper()
                                                .getContactData(contactId);
        final var contactDTO = ConvertCommDataToDTO.convertFrom(contactData);

        contactToAdd = !checkContactOnline(contactId) && contactAdded;
        contactStatusContent = new ContactMessengerStatusDTO(
                EligibleContactEntity.CLIENT, contactToAdd, contactDTO,
                Collections.emptyList());

        this.contentHandler.addResponse(contactStatusContent);
    }

    private
    boolean checkContactOnline (final int contactId) {
        return !(this.threadDataAccess.getContactToActiveClientMapper()
                                      .removeOfflineContacts(Set.of(contactId))
                                      .isEmpty());
    }
}