package de.vsy.server.client_handling.packet_processing.processor;

import de.vsy.server.server_packet.content.SimpleInternalContentWrapper;
import de.vsy.shared_module.shared_module.packet_exception.PacketProcessingException;
import de.vsy.shared_module.shared_module.packet_exception.PacketValidationException;
import de.vsy.shared_module.shared_module.packet_processing.PacketProcessor;
import de.vsy.shared_transmission.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.shared_transmission.packet.content.PacketContent;
import de.vsy.shared_transmission.shared_transmission.packet.property.packet_identifier.ContentIdentifier;

public class ClientPacketProcessorLink extends AbstractPacketProcessorLink {

  private final PacketProcessorManager processingLogic;

  public ClientPacketProcessorLink(final PacketProcessorManager processingLogic) {
    super(null);
    this.processingLogic = processingLogic;
  }

  @Override
  public void processPacket(Packet input)
      throws PacketValidationException, PacketProcessingException {
    PacketProcessor processor;
    final var inputContent = input.getPacketContent();
    final ContentIdentifier identifier;
    final Class<? extends PacketContent> contentType;

    if (inputContent instanceof final SimpleInternalContentWrapper inputServerContent) {
      contentType = inputServerContent.getWrappedContent().getClass();
    } else {
      contentType = inputContent.getClass();
    }
    identifier = input.getPacketProperties().getPacketIdentificationProvider();

    processor = this.processingLogic.getProcessor(identifier, contentType)
        .orElseThrow(() -> new PacketProcessingException(
            "Es wurde keine Verarbeitungslogik" + "für die folgende Kennzeichnung " + "gefunden"
                + input.getPacketProperties().getPacketIdentificationProvider()));
    processor.processPacket(input);
  }
}
