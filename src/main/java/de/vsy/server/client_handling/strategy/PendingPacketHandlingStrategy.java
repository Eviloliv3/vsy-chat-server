package de.vsy.server.client_handling.strategy;

import de.vsy.shared_transmission.packet.Packet;

/**
 * The Interface PendingPacketHandlingStrategy.
 */
public interface PendingPacketHandlingStrategy {

  /**
   * Handle Packet
   *
   * @param input the input
   */
  void handlePacket(Packet input);
}
