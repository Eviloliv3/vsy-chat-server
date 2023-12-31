package de.vsy.server.client_handling.packet_processing.content_processing;

import de.vsy.server.client_handling.data_management.AuthenticationHandlerDataProvider;
import de.vsy.server.client_handling.data_management.logic.AuthenticationStateControl;
import de.vsy.server.data.access.CommunicatorDataManipulator;
import de.vsy.server.data.access.HandlerAccessManager;
import de.vsy.server.persistent_data.PersistentDataLocationRemover;
import de.vsy.server.persistent_data.client_data.ContactListDAO;
import de.vsy.server.server_packet.packet_creation.ResultingPacketContentHandler;
import de.vsy.shared_module.packet_management.ClientDataProvider;
import de.vsy.shared_module.packet_processing.ContentProcessor;
import de.vsy.shared_transmission.packet.content.authentication.AccountDeletionRequestDTO;
import de.vsy.shared_transmission.packet.content.authentication.AccountDeletionResponseDTO;
import de.vsy.shared_transmission.packet.content.relation.ContactRelationRequestDTO;
import de.vsy.shared_transmission.packet.content.relation.EligibleContactEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.util.EnumMap;
import java.util.Set;

import static de.vsy.server.client_management.ClientState.AUTHENTICATED;
import static de.vsy.server.persistent_data.DataPathType.EXTENDED;
import static de.vsy.shared_transmission.packet.property.communicator.CommunicationEndpoint.getClientEntity;
import static de.vsy.shared_utility.standard_value.ThreadContextValues.LOG_FILE_CONTEXT_KEY;

public class AccountDeletionProcessor implements ContentProcessor<AccountDeletionRequestDTO> {
    private static final Logger LOGGER = LogManager.getLogger();
    private final CommunicatorDataManipulator clientRegistry;
    private final ClientDataProvider clientDataProvider;
    private final AuthenticationStateControl clientStateManager;
    private final ResultingPacketContentHandler contentHandler;
    private final ContactListDAO contactList;

    public AccountDeletionProcessor(AuthenticationHandlerDataProvider authenticationDataProvider) {
        clientRegistry = HandlerAccessManager.getCommunicatorDataManipulator();
        clientDataProvider = authenticationDataProvider.getLocalClientDataProvider();
        clientStateManager = authenticationDataProvider.getAuthenticationStateControl();
        contentHandler = authenticationDataProvider.getResultingPacketContentHandler();
        contactList = authenticationDataProvider.getLocalClientStateObserverManager().getClientPersistentAccess()
                .getContactListDAO();
    }

    @Override
    public void processContent(AccountDeletionRequestDTO toProcess) {
        final boolean accountDeleted;
        final int clientId = clientDataProvider.getClientId();

        if (clientRegistry.deleteAccount(clientId)) {
            accountDeleted = true;
            removeRelationships();

            if (clientStateManager.changePersistentClientState(AUTHENTICATED, false)) {
                this.clientStateManager.appendSynchronizationRemovalPacketPerState();
                ThreadContext.put(LOG_FILE_CONTEXT_KEY, Thread.currentThread().getName());
                clientStateManager.setAccountDeletionState(true);
                PersistentDataLocationRemover.deleteDirectories(EXTENDED, String.valueOf(clientId), LOGGER);
            } else {
                LOGGER.warn("Client could not be logged out globally.");
            }
        } else {
            LOGGER.warn("Account deletion failed for id {}.", clientId);
            accountDeleted = false;
        }
        contentHandler.addResponse(new AccountDeletionResponseDTO(accountDeleted));
    }

    private void removeRelationships() {
        final var clientData = this.clientDataProvider.getCommunicatorData();
        final EnumMap<EligibleContactEntity, Set<Integer>> contacts = new EnumMap<>(EligibleContactEntity.class);

        contacts.putAll(this.contactList.readContactMap());

        for (var contactSet : contacts.entrySet()) {
            final var contactType = contactSet.getKey();
            contactSet.getValue().forEach((contactId) -> {
                var request = new ContactRelationRequestDTO(contactType, clientData.getCommunicatorId(), contactId, clientData, false);
                this.contentHandler.addRequest(request, getClientEntity(contactId));
                this.contactList.removeContactFromSet(contactType, contactId);
            });
        }
    }
}
