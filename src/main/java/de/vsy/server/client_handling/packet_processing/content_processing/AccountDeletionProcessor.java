package de.vsy.server.client_handling.packet_processing.content_processing;

import static de.vsy.server.client_management.ClientState.ACTIVE_MESSENGER;
import static de.vsy.shared_transmission.packet.content.error.ErrorContent.ErrorDTO;
import static de.vsy.shared_transmission.packet.content.relation.EligibleContactEntity.CLIENT;
import static de.vsy.shared_utility.standard_value.ThreadContextValues.LOG_FILE_CONTEXT_KEY;

import de.vsy.server.client_handling.data_management.access_limiter.AuthenticationHandlingDataProvider;
import de.vsy.server.client_handling.data_management.logic.AuthenticationStateControl;
import de.vsy.server.client_management.ClientState;
import de.vsy.server.data.access.CommunicatorDataManipulator;
import de.vsy.server.data.access.HandlerAccessManager;
import de.vsy.server.persistent_data.client_data.ContactListDAO;
import de.vsy.server.server_packet.packet_creation.ResultingPacketContentHandler;
import de.vsy.shared_module.packet_exception.PacketProcessingException;
import de.vsy.shared_module.packet_management.ClientDataProvider;
import de.vsy.shared_module.packet_processing.ContentProcessor;
import de.vsy.shared_transmission.dto.CommunicatorDTO;
import de.vsy.shared_transmission.packet.content.authentication.AccountDeletionRequestDTO;
import de.vsy.shared_transmission.packet.content.authentication.AccountDeletionResponseDTO;
import de.vsy.shared_transmission.packet.content.relation.ContactRelationRequestDTO;
import de.vsy.shared_transmission.packet.content.status.ContactMessengerStatusDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

public class AccountDeletionProcessor implements ContentProcessor<AccountDeletionRequestDTO> {
  private static final Logger LOGGER = LogManager.getLogger();
  private final CommunicatorDataManipulator clientRegistry;
  private final ClientDataProvider clientDataProvider;
  private final AuthenticationStateControl clientStateManager;
  private final ResultingPacketContentHandler contentHandler;
  private final ContactListDAO contactList;

  public AccountDeletionProcessor(AuthenticationHandlingDataProvider authenticationDataProvider){
    clientRegistry = HandlerAccessManager.getCommunicatorDataManipulator();
    clientDataProvider = authenticationDataProvider.getLocalClientDataProvider();
    clientStateManager = authenticationDataProvider.getGlobalAuthenticationStateControl();
    contentHandler = authenticationDataProvider.getResultingPacketContentHandler();
    contactList = authenticationDataProvider.getLocalClientStateDependentLogicProvider().getClientPersistentAccess()
        .getContactListDAO();
  }

  @Override
  public void processContent(AccountDeletionRequestDTO toProcess) throws PacketProcessingException {
    //TODO Ablauf pruefen
    final boolean accountDeleted;
    final CommunicatorDTO  clientData =  clientDataProvider.getCommunicatorData();
    final int clientId = clientDataProvider.getClientId();

    if(clientRegistry.deleteAccount(clientId)){
      accountDeleted = true;
      createRelationshipTerminationRequests();

      if(clientStateManager.changePersistentClientState(ClientState.AUTHENTICATED, false)){
        ThreadContext.put(LOG_FILE_CONTEXT_KEY, Thread.currentThread().getName());
        clientStateManager.logoutClient();
      }else{
        LOGGER.warn("Client could not be logged out globally.");
      }
    }else{
      LOGGER.warn("Account deletion failed for id {}.", clientId);
      accountDeleted = false;
    }
    contentHandler.addResponse(new AccountDeletionResponseDTO(accountDeleted));
  }

  private void createRelationshipTerminationRequests(){
    final var clientData = this.clientDataProvider.getCommunicatorData();
    final var clientContactSet = this.contactList.readContacts(CLIENT);

    for(var contactId : clientContactSet){
      var request = new ContactRelationRequestDTO(CLIENT, clientData.getCommunicatorId(), contactId, clientData, false);
      this.contentHandler.addRequest(request);
    }
  }
}
