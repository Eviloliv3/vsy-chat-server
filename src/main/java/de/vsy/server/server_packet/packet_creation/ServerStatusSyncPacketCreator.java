package de.vsy.server.server_packet.packet_creation;

import de.vsy.server.client_handling.packet_processing.processor.ResultingPacketCreator;
import de.vsy.server.server_packet.content.ServerPacketContentImpl;
import de.vsy.shared_module.packet_creation.PacketCompiler;
import de.vsy.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.packet.content.PacketContent;
import de.vsy.shared_transmission.packet.property.communicator.CommunicationEndpoint;

public class ServerStatusSyncPacketCreator extends ResultingPacketCreator {

  @Override
  public Packet createRequest(PacketContent processedContent, CommunicationEndpoint recipient) {
    if (processedContent == null) {
      throw new IllegalArgumentException("Kein Paketinhalt uebergeben.");
    }
    if (recipient == null) {
      throw new IllegalArgumentException("Kein Empfaenger uebergeben.");
    }
    final var wrappedContent = wrapIfNecessary(processedContent);

    if (this.currentRequest == null) {
      return PacketCompiler.createRequest(recipient, wrappedContent);
    } else {
      return PacketCompiler.createFollowUpRequest(recipient, wrappedContent, this.currentRequest);
    }
  }

  @Override
  public Packet createResponse(PacketContent processedContent) {
    if (processedContent == null) {
      throw new IllegalArgumentException("Kein Paketinhalt uebergeben.");
    }
    final var wrappedContent = wrapIfNecessary(processedContent);
    return PacketCompiler.createResponse(wrappedContent, super.currentRequest);
  }

  protected PacketContent wrapIfNecessary(PacketContent processedContent) {
    final PacketContent wrappedContent;

    if (processedContent instanceof ServerPacketContentImpl) {
      wrappedContent = processedContent;
    } else {
      wrappedContent = wrapContent(processedContent);
    }
    return wrappedContent;
  }
}
