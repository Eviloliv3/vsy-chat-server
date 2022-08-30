/*
 *
 */
package de.vsy.server.client_handling.packet_processing.content_processing;

import de.vsy.server.client_handling.data_management.logic.AuthenticationStateControl;
import de.vsy.server.server.client_management.ClientState;
import de.vsy.server.server_packet.packet_creation.ResultingPacketContentHandler;
import de.vsy.shared_module.shared_module.packet_exception.PacketProcessingException;
import de.vsy.shared_module.shared_module.packet_processing.ContentProcessor;
import de.vsy.server.client_handling.data_management.access_limiter.AuthenticationHandlingDataProvider;
import de.vsy.shared_transmission.shared_transmission.packet.content.authentication.LogoutRequestDTO;
import de.vsy.shared_transmission.shared_transmission.packet.content.authentication.LogoutResponseDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

public
class LogoutRequestProcessor implements ContentProcessor<LogoutRequestDTO> {

    private static final Logger LOGGER = LogManager.getLogger();
    private final AuthenticationStateControl clientStateManager;
    private final ResultingPacketContentHandler contentHandler;

    /**
     * Instantiates a new login PacketHandler.
     *
     * @param threadDataAccess the thread dataManagement accessLimiter
     */
    public
    LogoutRequestProcessor (
            final AuthenticationHandlingDataProvider threadDataAccess) {

        this.clientStateManager = threadDataAccess.getGlobalAuthenticationStateControl();
        this.contentHandler = threadDataAccess.getResultingPacketContentHandler();
    }

    @Override
    public
    void processContent (LogoutRequestDTO toProcess)
    throws PacketProcessingException {
        String causeMessage = null;

        if (this.clientStateManager.changePersistentClientState(
                ClientState.AUTHENTICATED, false)) {
            ThreadContext.put("logFilename", Thread.currentThread().getName());

            this.contentHandler.addResponse(new LogoutResponseDTO(true));
            this.clientStateManager.logoutClient();
        } else {
            causeMessage = "Es ist ein Fehler beim Eintragen Ihres " +
                           "Authentifizierungszustandes aufgetreten. " +
                           "(Logout-global) Bitte melden Sie dies einem " +
                           "ChatServer-Mitarbeiter";
        }
        if (causeMessage != null) {
            throw new PacketProcessingException(causeMessage);
        }
    }
}
