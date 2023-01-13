/*
 *
 */
package de.vsy.server.client_handling.packet_processing.content_processing;

import de.vsy.server.client_handling.data_management.AuthenticationHandlerDataProvider;
import de.vsy.server.client_handling.data_management.logic.AuthenticationStateControl;
import de.vsy.server.data.access.CommunicatorDataManipulator;
import de.vsy.server.data.access.HandlerAccessManager;
import de.vsy.server.persistent_data.data_bean.ConvertCommDataToDTO;
import de.vsy.server.server_packet.packet_creation.ResultingPacketContentHandler;
import de.vsy.shared_module.packet_exception.PacketProcessingException;
import de.vsy.shared_module.packet_processing.ContentProcessor;
import de.vsy.shared_transmission.dto.CommunicatorDTO;
import de.vsy.shared_transmission.packet.content.authentication.AccountCreationRequestDTO;
import de.vsy.shared_transmission.packet.content.authentication.LoginResponseDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static de.vsy.server.client_management.ClientState.AUTHENTICATED;

public class AccountCreationProcessor implements ContentProcessor<AccountCreationRequestDTO> {

    private static final Logger LOGGER = LogManager.getLogger();
    private final CommunicatorDataManipulator clientRegistry;
    private final AuthenticationStateControl clientStateManager;
    private final ResultingPacketContentHandler contentHandler;

    /**
     * Instantiates a new login PacketHandler.
     *
     * @param threadDataAccess the thread dataManagement accessLimiter
     */
    public AccountCreationProcessor(final AuthenticationHandlerDataProvider threadDataAccess) {

        this.clientStateManager = threadDataAccess.getAuthenticationStateControl();
        this.clientRegistry = HandlerAccessManager.getCommunicatorDataManipulator();
        this.contentHandler = threadDataAccess.getResultingPacketContentHandler();
    }

    @Override
    public void processContent(AccountCreationRequestDTO toProcess) throws PacketProcessingException {
        String causeMessage = null;
        var newAccount = toProcess.getAccountCreationData();
        var clientData = clientRegistry.createNewAccount(
                newAccount.getAuthenticationData().getUsername(),
                newAccount.getAuthenticationData().getPassword(),
                (newAccount.getPersonalData().getFirstName() + " " + newAccount.getPersonalData()
                        .getLastName()));

        if (clientData != null) {

            if (this.clientStateManager.registerClient(clientData)) {

                if (this.clientStateManager.changePersistentClientState(AUTHENTICATED, true)) {
                    final CommunicatorDTO communicatorData = ConvertCommDataToDTO.convertFrom(clientData);
                    this.clientStateManager.appendStateSynchronizationPacket(AUTHENTICATED, true);
                    this.contentHandler.addResponse(new LoginResponseDTO(communicatorData));
                } else {
                    this.clientStateManager.deregisterClient();
                    causeMessage = "An error occurred while writing your global login state. Please contact the ChatServer support team.";
                }
            } else {
                this.clientStateManager.deregisterClient();
                causeMessage =
                        "An error occurred while writing your local login state. Please contact the ChatServer support team.";
            }
        } else {
            LOGGER.error("Account not created for input: {}.", toProcess);
            causeMessage = "No account was created. There is an existing account with the provided login data.";
        }
        if (causeMessage != null) {
            throw new PacketProcessingException(causeMessage);
        }
    }
}
