/*
 *
 */
package de.vsy.server.client_handling.packet_processing.content_processing;

import static de.vsy.shared_utility.standard_value.ThreadContextValues.LOG_FILE_CONTEXT_KEY;

import de.vsy.server.client_handling.data_management.access_limiter.AuthenticationHandlingDataProvider;
import de.vsy.server.client_handling.data_management.logic.AuthenticationStateControl;
import de.vsy.server.client_management.ClientState;
import de.vsy.server.server_packet.packet_creation.ResultingPacketContentHandler;
import de.vsy.shared_module.packet_exception.PacketProcessingException;
import de.vsy.shared_module.packet_processing.ContentProcessor;
import de.vsy.shared_transmission.packet.content.authentication.LogoutRequestDTO;
import de.vsy.shared_transmission.packet.content.authentication.LogoutResponseDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

public class LogoutRequestProcessor implements ContentProcessor<LogoutRequestDTO> {

  private static final Logger LOGGER = LogManager.getLogger();
  private final AuthenticationStateControl clientStateManager;
  private final ResultingPacketContentHandler contentHandler;

  /**
   * Instantiates a new login PacketHandler.
   *
   * @param threadDataAccess the thread dataManagement accessLimiter
   */
  public LogoutRequestProcessor(final AuthenticationHandlingDataProvider threadDataAccess) {

    this.clientStateManager = threadDataAccess.getGlobalAuthenticationStateControl();
    this.contentHandler = threadDataAccess.getResultingPacketContentHandler();
  }

  @Override
  public void processContent(LogoutRequestDTO toProcess) throws PacketProcessingException {
    String causeMessage = null;

    if (this.clientStateManager.changePersistentClientState(ClientState.AUTHENTICATED, false)) {
      ThreadContext.put(LOG_FILE_CONTEXT_KEY, Thread.currentThread().getName());

      this.contentHandler.addResponse(new LogoutResponseDTO(true));
      this.clientStateManager.logoutClient();
    } else {
      causeMessage =
          "An error occurred while writing your global logout state. Please contact the ChatServer support team.";
    }
    if (causeMessage != null) {
      throw new PacketProcessingException(causeMessage);
    }
  }
}
