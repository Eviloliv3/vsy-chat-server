package de.vsy.server.client_handling.packet_processing.content_processing;

import de.vsy.server.client_handling.data_management.AuthenticationHandlerDataProvider;
import de.vsy.server.client_handling.data_management.PendingPacketCleaner;
import de.vsy.server.client_handling.data_management.logic.AuthenticationStateControl;
import de.vsy.server.client_management.ClientState;
import de.vsy.server.data.access.CommunicatorDataManipulator;
import de.vsy.server.data.access.HandlerAccessManager;
import de.vsy.server.persistent_data.client_data.PendingPacketDAO;
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
    private final PendingPacketDAO pendingPacketProvider;

    /**
     * Instantiates a new login PacketHandler.
     *
     * @param threadDataAccess the thread dataManagement accessLimiter
     */
    public LoginRequestProcessor(final AuthenticationHandlerDataProvider threadDataAccess) {
        this.clientStateManager = threadDataAccess.getAuthenticationStateControl();
        this.commPersistManager = HandlerAccessManager.getCommunicatorDataManipulator();
        this.contentHandler = threadDataAccess.getResultingPacketContentHandler();
        this.pendingPacketProvider = threadDataAccess.getPendingPacketDAO();
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
            this.clientStateManager.registerClient(clientData);

            if (!(this.clientStateManager.checkGlobalClientState(AUTHENTICATED))) {

                if (this.clientStateManager.changePersistentClientState(AUTHENTICATED, true)) {
                    final var communicatorData = ConvertCommDataToDTO.convertFrom(clientData);
                    this.clientStateManager.appendStateSynchronizationPacket(AUTHENTICATED, true);
                    this.contentHandler.addResponse(new LoginResponseDTO(communicatorData));
                    PendingPacketCleaner.removeVolatilePackets(this.pendingPacketProvider);
                } else {
                    this.clientStateManager.deregisterClient();
                    causeMessage = "An error occurred while writing your global login state. Please contact the ChatServer support team.";
                }
            } else {
                this.clientStateManager.deregisterClient();
                causeMessage = "You already are connected from another device.";
            }
        } else {
            causeMessage = "No account data found for your credentials.";
        }

        if (causeMessage != null) {
            throw new PacketProcessingException(causeMessage);
        }
    }
}
