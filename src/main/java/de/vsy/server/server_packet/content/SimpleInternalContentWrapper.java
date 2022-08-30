package de.vsy.server.server_packet.content;

import de.vsy.server.server_packet.content.builder.SimpleInternalContentBuilder;
import de.vsy.shared_transmission.shared_transmission.packet.content.PacketContent;

import java.io.Serial;

/**
 * The Class SimpleInternalContentWrapper.
 *
 * @author Frederic Heath
 */
public
class SimpleInternalContentWrapper extends ServerPacketContentImpl {

    @Serial
    private static final long serialVersionUID = -7878330046557483936L;
    private final PacketContent wrappedContent;

    /**
     * Instantiates a new simple internal content wrapper.
     *
     * @param builder the builder
     */
    public
    SimpleInternalContentWrapper (SimpleInternalContentBuilder builder) {
        super(builder);
        this.wrappedContent = builder.getWrappedContent();
    }

    public
    PacketContent getWrappedContent () {
        return this.wrappedContent;
    }

    @Override
    public
    String toString () {
        return "\"simpleContentWrapper\": { " + super.toString() +
               ", \"wrappedContent\": " + this.wrappedContent + " }";
    }
}
