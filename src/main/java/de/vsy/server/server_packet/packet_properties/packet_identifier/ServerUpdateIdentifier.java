/*
 *
 */
package de.vsy.server.server_packet.packet_properties.packet_identifier;

import static de.vsy.shared_transmission.shared_transmission.packet.property.packet_category.PacketCategory.STATUS;

import java.io.Serial;

import de.vsy.shared_transmission.shared_transmission.packet.property.packet_category.PacketCategory;
import de.vsy.shared_transmission.shared_transmission.packet.property.packet_identifier.ContentIdentifierImpl;
import de.vsy.shared_transmission.shared_transmission.packet.property.packet_type.PacketType;

public class ServerUpdateIdentifier extends ContentIdentifierImpl {

	private static final PacketCategory CATEGORY = STATUS;
	@Serial
	private static final long serialVersionUID = 1731415962240477462L;

	/**
	 * Instantiates a new status sync identifier.
	 *
	 * @param type the type
	 */
	public ServerUpdateIdentifier(final PacketType type) {
		super(CATEGORY, type);
	}
}
