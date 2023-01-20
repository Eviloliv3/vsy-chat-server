
package de.vsy.server.client_handling.packet_processing.content_processing;

import de.vsy.server.client_handling.data_management.NotificationHandlingDataProvider;
import de.vsy.server.server_packet.packet_creation.ResultingPacketContentHandler;
import de.vsy.shared_module.packet_processing.ContentProcessor;
import de.vsy.shared_transmission.packet.content.error.ErrorDTO;

public class ErrorTransmissionProcessor implements ContentProcessor<ErrorDTO> {

    private final ResultingPacketContentHandler contentHandler;

    /**
     * Instantiates a new error response transmission handler.
     */
    public ErrorTransmissionProcessor(final NotificationHandlingDataProvider threadDataAccess) {
        this.contentHandler = threadDataAccess.getResultingPacketContentHandler();
    }

    @Override
    public void processContent(ErrorDTO validatedContent) {
        this.contentHandler.addRequest(validatedContent);
    }
}
