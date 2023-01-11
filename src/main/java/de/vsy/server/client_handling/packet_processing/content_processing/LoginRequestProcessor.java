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
import de.vsy.shared_transmission.packet.content.authentication.LoginRequestDTO;
import de.vsy.shared_transmission.packet.content.authentication.LoginResponseDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static de.vsy.server.client_management.ClientState.AUTHENTICATED;

/**
 * PacketProcessor for login type Packet.
 */
public class LoginRequestProcessor implements ContentProcessor<LoginRequestDTO> {

    private static final Logger LOGGER = LogManager.getLogger();
    private final AuthenticationStateControl clientStateManager;
    private final CommunicatorDataManipulator commPersistManager;
    private final ResultingPacketContentHandler contentHandler;

    /**
     * Instantiates a new login PacketHandler.
     *
     * @param threadDataAccess the thread dataManagement accessLimiter
     */
    public LoginRequestProcessor(final AuthenticationHandlingDataProvider threadDataAccess) {

        this.clientStateManager = threadDataAccess.getGlobalAuthenticationStateControl();
        this.commPersistManager = HandlerAccessManager.getCommunicatorDataManipulator();
        this.contentHandler = threadDataAccess.getResultingPacketContentHandler();
    }

    @Override
    public void processContent(LoginRequestDTO toProcess) throws PacketProcessingException {
        String causeMessage = null;
        ClientState globalState;
        final var authenticationData = toProcess.getAuthenticationData();
        final var clientData = this.commPersistManager.getCommunicatorData(
                authenticationData.getUsername(),
                authenticationData.getPassword());

        if (clientData != null) {

            if (this.clientStateManager.registerClient(clientData)) {
                globalState = this.clientStateManager.getPersistentClientState();

                if (globalState.equals(ClientState.NOT_AUTHENTICATED)) {

                    if (this.clientStateManager.changePersistentClientState(AUTHENTICATED,
                            true)) {
                        final var communicatorData = ConvertCommDataToDTO.convertFrom(clientData);
                        this.clientStateManager.appendStateSynchronizationPacket(AUTHENTICATED, true);
                        this.contentHandler.addResponse(new LoginResponseDTO(communicatorData));
                    } else {
                        this.clientStateManager.deregisterClient();
                        causeMessage = "An error occurred while writing your global login state. Please contact the ChatServer support team.";
                    }
                } else {
                    causeMessage = "You already are connected from another device.";
                }
            } else {
                this.clientStateManager.deregisterClient();
                causeMessage =
                        "An error occurred while writing your local login state. Please contact the ChatServer support team.";
            }
        } else {
            causeMessage = "No account data found for your credentials.";
        }
        if (causeMessage != null) {
            throw new PacketProcessingException(causeMessage);
        }
    }
}
