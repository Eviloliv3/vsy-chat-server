/*
 *
 */
package de.vsy.chat.server.server_test_helpers;

import de.vsy.shared_transmission.shared_transmission.packet.property.PacketProperties;
import de.vsy.shared_transmission.shared_transmission.packet.property.packet_identifier.ContentIdentifier;

/**
 * Provides PacketProperties for a distinct identifier and a contactId.
 */
public interface PacketPropertiesProvider {

  /**
   * Creates the de.vsy.shared_transmission.packet.property.
   *
   * @param identifier the identifier
   * @param contactId  the contact id
   * @return the PacketProperties
   */
  PacketProperties createPacketProperties(ContentIdentifier identifier, int contactId);
}
