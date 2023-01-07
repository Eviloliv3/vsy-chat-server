package de.vsy.server.server_packet.packet_properties.packet_identifier;

import de.vsy.shared_transmission.packet.property.packet_category.PacketCategory;
import de.vsy.shared_transmission.packet.property.packet_identifier.ContentIdentifierImpl;
import de.vsy.shared_transmission.packet.property.packet_type.PacketType;

import java.io.Serial;

import static de.vsy.shared_transmission.packet.property.packet_category.PacketCategory.NOTIFICATION;

/**
 * The Class ServerErrorIdentifier.
 */
public class ServerErrorIdentifier extends ContentIdentifierImpl {

    private static final PacketCategory CATEGORY = NOTIFICATION;
    @Serial
    private static final long serialVersionUID = 1731415962240477462L;

    /**
     * Instantiates a new status sync identifier.
     *
     * @param type the type
     */
    public ServerErrorIdentifier(final PacketType type) {
        super(CATEGORY, type);
    }
}
