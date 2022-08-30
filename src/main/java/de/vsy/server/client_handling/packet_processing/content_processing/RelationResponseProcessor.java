package de.vsy.server.client_handling.packet_processing.content_processing;

import de.vsy.server.persistent_data.client_data.ContactListDAO;
import de.vsy.server.persistent_data.client_data.MessageDAO;
import de.vsy.server.persistent_data.data_bean.ConvertCommDataToDTO;
import de.vsy.server.persistent_data.manipulation_utility.RelationManipulator;
import de.vsy.server.server_packet.packet_creation.ResultingPacketContentHandler;
import de.vsy.shared_module.shared_module.packet_exception.PacketProcessingException;
import de.vsy.shared_module.shared_module.packet_processing.ContentProcessor;
import de.vsy.server.client_handling.data_management.access_limiter.RelationHandlingDataProvider;
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

        final var errorMessage = checkResponseLegitimacy(extractedContent,
                                                         contactId);
        if (errorMessage == null) {

            final var isFriendshipRequest = extractedContent.getDesiredState();
            final var iAmOriginator = this.checkClientOriginator(extractedContent);

            if (isFriendshipRequest && extractedContent.getDecision()) {
                RelationManipulator.addContact(extractedContent.getContactType(),
                                               contactId, this.contactListAccess);
                this.appendStatusMessage(contactId, true);
            } else if (iAmOriginator && !isFriendshipRequest) {
                RelationManipulator.removeContact(extractedContent.getContactType(),
                                                  contactId, this.contactListAccess,
                                                  this.messageHistoryAccess);
                this.appendStatusMessage(contactId, false);
            }
            this.contentHandler.addResponse(extractedContent);
        } else {
            throw new PacketProcessingException(errorMessage);
        }
    }

    private
    int extractContactId (final ContactRelationResponseDTO responseData) {
        final var originatorId = responseData.getOriginatorId();
        final var recipientId = responseData.getRecipientId();
        final var clientId = this.threadDataAccess.getLocalClientDataProvider()
                                                  .getClientId();

        return IdComparator.determineContactId(clientId, originatorId, recipientId);
    }

    /**
     * Check request legitimacy.
     *
     * @param contactRequest the contact request
     */
    private
    String checkResponseLegitimacy (final ContactRelationResponseDTO contactRequest,
                                    final int contactId) {
        String errorMessage = null;
        final var recipientId = contactRequest.getRecipientId();
        final var desiredState = contactRequest.getDesiredState();
        final var contactsAlready = this.contactListAccess.checkAcquaintance(
                contactRequest.getContactType(), recipientId);

        if (desiredState && contactsAlready) {
            final var contactData = this.threadDataAccess.getContactToActiveClientMapper()
                                                         .getContactData(contactId);
            errorMessage = "Freundschaftsanfrage wurde nicht verarbeitet. Sie " +
                           "sind bereits mit " + contactData.getDisplayName() +
                           " befreundet.";
        }
        return errorMessage;
    }

    private
    boolean checkClientOriginator (final ContactRelationResponseDTO responseData) {
        final var clientId = this.threadDataAccess.getLocalClientDataProvider()
                                                  .getClientId();
        final var originatorId = responseData.getOriginatorId();
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
