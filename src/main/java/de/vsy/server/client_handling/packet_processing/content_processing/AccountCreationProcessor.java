/*
 *
 */
package de.vsy.server.client_handling.packet_processing.content_processing;

import de.vsy.server.client_handling.data_management.access_limiter.AuthenticationHandlingDataProvider;
import de.vsy.server.client_handling.data_management.logic.AuthenticationStateControl;
import de.vsy.server.client_management.ClientState;
import de.vsy.server.data.access.CommunicatorDataManipulator;
import de.vsy.server.data.access.HandlerAccessManager;
import de.vsy.server.persistent_data.data_bean.ConvertCommDataToDTO;
import de.vsy.server.server_packet.packet_creation.ResultingPacketContentHandler;
import de.vsy.shared_module.packet_exception.PacketProcessingException;
import de.vsy.shared_module.packet_processing.ContentProcessor;
import de.vsy.shared_transmission.dto.CommunicatorDTO;
import de.vsy.shared_transmission.packet.content.authentication.LoginResponseDTO;
import de.vsy.shared_transmission.packet.content.authentication.NewAccountRequestDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AccountCreationProcessor implements ContentProcessor<NewAccountRequestDTO> {

  private static final Logger LOGGER = LogManager.getLogger();
  private final CommunicatorDataManipulator clientRegistry;
  private final AuthenticationStateControl clientStateManager;
  private final ResultingPacketContentHandler contentHandler;

  /**
   * Instantiates a new login PacketHandler.
   *
   * @param threadDataAccess the thread dataManagement accessLimiter
   */
  public AccountCreationProcessor(final AuthenticationHandlingDataProvider threadDataAccess) {

    this.clientStateManager = threadDataAccess.getGlobalAuthenticationStateControl();
    this.clientRegistry = HandlerAccessManager.getCommunicatorDataManipulator();
    this.contentHandler = threadDataAccess.getResultingPacketContentHandler();
  }

  @Override
  public void processContent(NewAccountRequestDTO toProcess) throws PacketProcessingException {
    String causeMessage = null;
    var newAccount = toProcess.getAccountCreationData();
    var clientData = clientRegistry.createNewAccount(newAccount.getAuthenticationData().getUsername(),
        newAccount.getAuthenticationData().getPassword(),
        (newAccount.getPersonalData().getForename() + " " + newAccount.getPersonalData()
            .getSurname()));

    if (clientData != null) {

      if (this.clientStateManager.loginClient(clientData)) {

        if (this.clientStateManager.changePersistentClientState(ClientState.AUTHENTICATED, true)) {
          final CommunicatorDTO communicatorData = ConvertCommDataToDTO.convertFrom(clientData);
          this.contentHandler.addResponse(new LoginResponseDTO(communicatorData));
        } else {
          this.clientStateManager.logoutClient();
          causeMessage = "An error occurred while writing your global login state. Please contact the ChatServer support team.";
        }
      } else {
        this.clientStateManager.logoutClient();
        causeMessage =
            "An error occurred while writing your local login state. Please contact the ChatServer support team.";
      }
    } else {
      causeMessage = "No account was created. There is an existing account with the provided login data.";
    }
    if (causeMessage != null) {
      throw new PacketProcessingException(causeMessage);
    }
  }
}
