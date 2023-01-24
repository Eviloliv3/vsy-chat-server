package de.vsy.server.service.packet_logic.processor;

import de.vsy.server.server_packet.content.SimpleInternalContentWrapper;
import de.vsy.server.server_packet.packet_creation.ResultingPacketContentHandler;
import de.vsy.server.service.packet_logic.ServicePacketProcessorFactory;
import de.vsy.shared_module.packet_exception.PacketHandlingException;
import de.vsy.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.packet.PacketBuilder;
import de.vsy.shared_transmission.packet.content.notification.ErrorDTO;

/**
 * Basic Packet processor using the strategy that is passed through the constructor.
 */
public class ServicePacketProcessor {

    private final ServicePacketProcessorFactory sphf;
    private final ResultingPacketContentHandler contentHandler;

    /**
     * Instantiates a new service packet processor.
     *
     * @param sphf the sphf
     */
    public ServicePacketProcessor(final ServicePacketProcessorFactory sphf,
                                  final ResultingPacketContentHandler contentHandler) {
        super();
        this.sphf = sphf;
        this.contentHandler = contentHandler;
    }

    /**
     * Process Packet
     *
     * @param input the input
     */
    public void processPacket(final Packet input) {
        final var identifier = input.getPacketProperties().getPacketIdentificationProvider();
        final var packetType = identifier.getPacketType();
        final var ph = this.sphf.getPacketProcessor(packetType);

        if (ph != null) {
            try {
                ph.processPacket(input);
            } catch (final PacketHandlingException phe) {
                Packet origin;
                if(input.getPacketContent() instanceof SimpleInternalContentWrapper wrapper){
                    var content = wrapper.getWrappedContent();
                    origin = new PacketBuilder().withContent(content).withProperties(input.getPacketProperties()).withRequestPacket(input.getRequestPacketHash()).build();
                }else{
                    origin = input;
                }
                final var errorContent = new ErrorDTO(phe.getMessage(), origin);
                this.contentHandler.setError(errorContent);
            }
        } else {
            final var errorMessage = "No PacketProcessor found for ContentIdentifier: " + identifier;
            Packet origin;
            if(input.getPacketContent() instanceof SimpleInternalContentWrapper wrapper){
                var content = wrapper.getWrappedContent();
                origin = new PacketBuilder().withContent(content).withProperties(input.getPacketProperties()).withRequestPacket(input.getRequestPacketHash()).build();
            }else{
                origin = input;
            }
            final var errorContent = new ErrorDTO(errorMessage, origin);
            this.contentHandler.setError(errorContent);
        }
    }
}
