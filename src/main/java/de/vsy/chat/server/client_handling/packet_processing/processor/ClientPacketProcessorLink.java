package de.vsy.chat.server.client_handling.packet_processing.processor;

import de.vsy.chat.shared_module.packet_exception.PacketProcessingException;
import de.vsy.chat.shared_module.packet_exception.PacketValidationException;
import de.vsy.chat.shared_module.packet_processing.PacketProcessor;
import de.vsy.chat.server.server_packet.content.SimpleInternalContentWrapper;
import de.vsy.chat.shared_transmission.packet.Packet;
import de.vsy.chat.shared_transmission.packet.content.PacketContent;

public
class ClientPacketProcessorLink extends AbstractPacketProcessorLink {

    private final PacketProcessorManager processingLogic;

    public
    ClientPacketProcessorLink (final PacketProcessorManager processingLogic) {
        super(null);
        this.processingLogic = processingLogic;
    }

    @Override
    public
    void processPacket (Packet input)
    throws PacketValidationException, PacketProcessingException {
        PacketProcessor processor;
        final var inputContent = input.getPacketContent();
        final Class<? extends PacketContent> contentType;

        if (inputContent instanceof final SimpleInternalContentWrapper inputServerContent) {
            contentType = inputServerContent.getWrappedContent().getClass();
        } else {
            contentType = inputContent.getClass();
        }

        processor = this.processingLogic.getProcessor(
                input.getPacketProperties().getContentIdentifier(), contentType);

        if (processor != null) {
            processor.processPacket(input);
        } else {
            throw new PacketProcessingException("Es wurde keine Verarbeitungslogik" +
                                                "für die folgende Kennzeichnung " +
                                                "gefunden" +
                                                input.getPacketProperties()
                                                     .getContentIdentifier());
        }
    }
}
