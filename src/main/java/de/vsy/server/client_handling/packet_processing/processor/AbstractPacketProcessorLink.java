package de.vsy.server.client_handling.packet_processing.processor;

import de.vsy.shared_module.packet_processing.PacketProcessor;

public abstract class AbstractPacketProcessorLink implements PacketProcessor {

  protected final PacketProcessor nextLink;

  protected AbstractPacketProcessorLink(PacketProcessor nextLink) {
    this.nextLink = nextLink;
  }
}
