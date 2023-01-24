package de.vsy.server.client_handling.packet_processing.content_processing;

import de.vsy.server.client_handling.data_management.NotificationHandlingDataProvider;
import de.vsy.server.server_packet.packet_creation.ResultingPacketContentHandler;
import de.vsy.shared_module.packet_processing.ContentProcessor;
import de.vsy.shared_transmission.packet.content.notification.SimpleInformationDTO;

public class NotificationProcessor implements ContentProcessor<SimpleInformationDTO> {

    private final ResultingPacketContentHandler contentHandler;

    public NotificationProcessor(final NotificationHandlingDataProvider threadDataAccess) {
        this.contentHandler = threadDataAccess.getResultingPacketContentHandler();
    }

    @Override
    public void processContent(SimpleInformationDTO validatedContent) {
        this.contentHandler.addRequest(validatedContent);
    }
}
