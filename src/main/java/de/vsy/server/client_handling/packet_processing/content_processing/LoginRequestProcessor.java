package de.vsy.server.client_handling.packet_processing.content_processing;

import de.vsy.server.client_handling.data_management.access_limiter.AuthenticationHandlingDataProvider;
import de.vsy.server.client_handling.data_management.logic.AuthenticationStateControl;
import de.vsy.server.persistent_data.data_bean.ConvertCommDataToDTO;
import de.vsy.server.server.client_management.ClientState;
import de.vsy.server.server.data.access.CommunicatorDataManipulator;
import de.vsy.server.server.data.access.HandlerAccessManager;
import de.vsy.server.server_packet.packet_creation.ResultingPacketContentHandler;
import de.vsy.shared_module.shared_module.packet_exception.PacketProcessingException;
import de.vsy.shared_module.shared_module.packet_processing.ContentProcessor;
import de.vsy.shared_transmission.shared_transmission.packet.content.authentication.LoginRequestDTO;
import de.vsy.shared_transmission.shared_transmission.packet.content.authentication.LoginResponseDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

/** PacketProcessor for login type Packet. */
public
class LoginRequestProcessor implements ContentProcessor<LoginRequestDTO> {

    private static final Logger LOGGER = LogManager.getLogger();
    private final AuthenticationStateControl clientStateManager;
    private final CommunicatorDataManipulator commPersistManager;
    private final ResultingPacketContentHandler contentHandler;

    /**
     * Instantiates a new login PacketHandler.
     *
     * @param threadDataAccess the thread dataManagement accessLimiter
     */
    public
    LoginRequestProcessor (
            final AuthenticationHandlingDataProvider threadDataAccess) {

        this.clientStateManager = threadDataAccess.getGlobalAuthenticationStateControl();
        this.commPersistManager = HandlerAccessManager.getCommunicatorDataManipulator();
        this.contentHandler = threadDataAccess.getResultingPacketContentHandler();
    }

    @Override
    public
    void processContent (LoginRequestDTO toProcess)
    throws PacketProcessingException {
        String causeMessage = null;
        ClientState globalState;
        final var authenticationData = toProcess.getAuthenticationData();
        final var clientData = this.commPersistManager.getCommunicatorData(
                authenticationData.getLogin(), authenticationData.getPassword());

        if (clientData != null) {

            if (this.clientStateManager.loginClient(clientData)) {
                globalState = this.clientStateManager.getPersistentClientState();

                if (globalState == ClientState.OFFLINE) {

                    if (this.clientStateManager.changePersistentClientState(
                            ClientState.AUTHENTICATED, true)) {
                        final var communicatorData = ConvertCommDataToDTO.convertFrom(
                                clientData);
                        ThreadContext.put("logFilename", String.valueOf(
                                communicatorData.getCommunicatorId()));
                        this.contentHandler.addResponse(
                                new LoginResponseDTO(communicatorData));
                    } else {
                        this.clientStateManager.logoutClient();
                        causeMessage = "Es ist ein Fehler beim Eintragen Ihres " +
                                       "Authentifizierungszustandes aufgetreten. " +
                                       "(Login-global) Bitte melden Sie dies einem " +
                                       "ChatServer-Mitarbeiter";
                    }
                } else {
                    causeMessage = "Sie sind bereits von einem anderen Gerät aus angemeldet.";
                }
            } else {
                this.clientStateManager.logoutClient();
                causeMessage = "Es ist ein Fehler beim Eintragen Ihres " +
                               "Authentifizierungszustandes aufgetreten. " +
                               "(Login-lokal) Bitte melden Sie dies einem " +
                               "ChatServer-Mitarbeiter";
            }
        } else {
            causeMessage = "Es wurde kein Konto für die von Ihnen eingegebenen Login-Daten gefunden.";
        }
        if (causeMessage != null) {
            throw new PacketProcessingException(causeMessage);
        }
    }
}
