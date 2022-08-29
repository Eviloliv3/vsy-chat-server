/*
 *
 */
package de.vsy.chat.server.client_handling.packet_processing.content_processing;

import de.vsy.chat.shared_module.packet_exception.PacketProcessingException;
import de.vsy.chat.shared_module.packet_processing.ContentProcessor;
import de.vsy.chat.server.client_handling.data_management.access_limiter.AuthenticationHandlingDataProvider;
import de.vsy.chat.server.client_handling.data_management.logic.AuthenticationStateControl;
import de.vsy.chat.server.persistent_data.data_bean.ConvertCommDataToDTO;
import de.vsy.chat.server.server.client_management.ClientState;
import de.vsy.chat.server.server.data.access.CommunicatorDataManipulator;
import de.vsy.chat.server.server.data.access.HandlerAccessManager;
import de.vsy.chat.server.server_packet.packet_creation.ResultingPacketContentHandler;
import de.vsy.chat.shared_transmission.dto.CommunicatorDTO;
import de.vsy.chat.shared_transmission.packet.content.authentication.LoginResponseDTO;
import de.vsy.chat.shared_transmission.packet.content.authentication.NewAccountRequestDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

public
class NewAccountRequestProcessor implements ContentProcessor<NewAccountRequestDTO> {

    private static final Logger LOGGER = LogManager.getLogger();
    private final CommunicatorDataManipulator clientRegistry;
    private final AuthenticationStateControl clientStateManager;
    private final ResultingPacketContentHandler contentHandler;

    /**
     * Instantiates a new login PacketHandler.
     *
     * @param threadDataAccess the thread dataManagement accessLimiter
     */
    public
    NewAccountRequestProcessor (
            final AuthenticationHandlingDataProvider threadDataAccess) {

        this.clientStateManager = threadDataAccess.getGlobalAuthenticationStateControl();
        this.clientRegistry = HandlerAccessManager.getCommunicatorDataManipulator();
        this.contentHandler = threadDataAccess.getResultingPacketContentHandler();
    }

    @Override
    public
    void processContent (NewAccountRequestDTO toProcess)
    throws PacketProcessingException {
        String causeMessage = null;
        var newAccount = toProcess.getAccountCreationData();
        var clientData = clientRegistry.createNewAccount(
                newAccount.getAuthenticationData().getLogin(),
                newAccount.getAuthenticationData().getPassword(),
                (newAccount.getPersonalData().getForename() + " " +
                 newAccount.getPersonalData().getSurname()));

        if (clientData != null) {

            if (this.clientStateManager.loginClient(clientData)) {

                if (this.clientStateManager.changePersistentClientState(
                        ClientState.AUTHENTICATED, true)) {
                    final CommunicatorDTO communicatorData = ConvertCommDataToDTO.convertFrom(
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
                this.clientStateManager.logoutClient();
                causeMessage = "Es ist ein Fehler beim Eintragen Ihres " +
                               "Authentifizierungszustandes aufgetreten. " +
                               "(Login-lokal) Bitte melden Sie dies einem " +
                               "ChatServer-Mitarbeiter";
            }
        } else {
            causeMessage = "Es gibt bereits einen Account mit den, von Ihnen, eingegebenen Login-Daten.";
        }
        if (causeMessage != null) {
            throw new PacketProcessingException(causeMessage);
        }
    }
}
