/*
 *
 */
package de.vsy.chat.server.server_test_helpers;

import de.vsy.shared_transmission.packet.property.PacketProperties;
import de.vsy.shared_transmission.packet.property.packet_identifier.ContentIdentifier;

/**
 * Provides PacketProperties for a distinct identifier and a contactId.
 */
public interface PacketPropertiesProvider {

    /**
     * Creates PacketProperties depending on the Content Identifier and the
     * specified id.
     *
     * @param identifier the ContentIdentifier
     * @param contactId  the recipient id
     * @return the correspondingly created PacketProperties
     */
    PacketProperties createPacketProperties(ContentIdentifier identifier, int contactId);
}
