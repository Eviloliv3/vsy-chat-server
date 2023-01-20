
package de.vsy.server.client_handling.packet_processing.content_processing;

import de.vsy.server.client_handling.data_management.AuthenticationHandlerDataProvider;
import de.vsy.server.client_handling.data_management.logic.AuthenticationStateControl;
import de.vsy.server.server_packet.packet_creation.ResultingPacketContentHandler;
import de.vsy.shared_module.packet_exception.PacketProcessingException;
import de.vsy.shared_module.packet_processing.ContentProcessor;
import de.vsy.shared_transmission.packet.content.authentication.LogoutRequestDTO;
import de.vsy.shared_transmission.packet.content.authentication.LogoutResponseDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import static de.vsy.server.client_management.ClientState.AUTHENTICATED;
import static de.vsy.shared_utility.standard_value.ThreadContextValues.LOG_FILE_CONTEXT_KEY;

public class LogoutRequestProcessor implements ContentProcessor<LogoutRequestDTO> {

    private static final Logger LOGGER = LogManager.getLogger();
    private final AuthenticationStateControl clientStateManager;
    private final ResultingPacketContentHandler contentHandler;

    /**
     * Instantiates a new login PacketHandler.
     *
     * @param threadDataAccess the thread dataManagement accessLimiter
     */
    public LogoutRequestProcessor(final AuthenticationHandlerDataProvider threadDataAccess) {
        this.clientStateManager = threadDataAccess.getAuthenticationStateControl();
        this.contentHandler = threadDataAccess.getResultingPacketContentHandler();
    }

    @Override
    public void processContent(LogoutRequestDTO toProcess) throws PacketProcessingException {

        if (this.clientStateManager.changePersistentClientState(AUTHENTICATED, false)) {
            this.clientStateManager.appendSynchronizationRemovalPacketPerState();
            ThreadContext.put(LOG_FILE_CONTEXT_KEY, Thread.currentThread().getName());
            this.contentHandler.addResponse(new LogoutResponseDTO(true));
            this.clientStateManager.deregisterClient();
        } else {
            LOGGER.error("ClientState could not be changed from {}.", AUTHENTICATED);
            final var causeMessage =
                    "An error occurred while writing your global logout state. Please contact the ChatServer support team.";
            throw new PacketProcessingException(causeMessage);
        }
    }
}
