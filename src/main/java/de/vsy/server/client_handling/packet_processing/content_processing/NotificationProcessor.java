package de.vsy.server.client_handling.packet_processing.content_processing;

import de.vsy.server.client_handling.data_management.NotificationHandlingDataProvider;
import de.vsy.server.server_packet.content.SimpleInternalContentWrapper;
import de.vsy.server.server_packet.packet_creation.ResultingPacketContentHandler;
import de.vsy.shared_module.packet_processing.ContentProcessor;
import de.vsy.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.packet.PacketBuilder;
import de.vsy.shared_transmission.packet.content.PacketContent;
import de.vsy.shared_transmission.packet.content.notification.ErrorDTO;
import de.vsy.shared_transmission.packet.content.notification.SimpleInformationDTO;

public class NotificationProcessor implements ContentProcessor<SimpleInformationDTO> {

    private final ResultingPacketContentHandler contentHandler;

    public NotificationProcessor(final NotificationHandlingDataProvider threadDataAccess) {
        this.contentHandler = threadDataAccess.getResultingPacketContentHandler();
    }

    @Override
    public void processContent(SimpleInformationDTO validatedContent) {
        if(validatedContent instanceof final ErrorDTO error){
            Packet origin = error.getOriginPacket();

            if(origin != null) {
                PacketContent originalContent = origin.getPacketContent();

                if(originalContent instanceof final SimpleInternalContentWrapper wrapper){
                    origin = new PacketBuilder().withProperties(origin.getPacketProperties()).withContent(wrapper.getWrappedContent()).withRequestPacket(origin.getRequestPacketHash()).build();
                    this.contentHandler.addRequest(new ErrorDTO(error.getInformationString(), origin));
                    return;
                }
            }
        }
        this.contentHandler.addRequest(validatedContent);
    }
}
