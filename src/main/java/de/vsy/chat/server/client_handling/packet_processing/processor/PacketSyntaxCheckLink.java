package de.vsy.chat.server.client_handling.packet_processing.processor;

import de.vsy.chat.shared_module.packet_exception.PacketProcessingException;
import de.vsy.chat.shared_module.packet_exception.PacketValidationException;
import de.vsy.chat.shared_module.packet_processing.PacketProcessor;
import de.vsy.chat.shared_module.packet_validation.PacketCheck;
import de.vsy.chat.shared_transmission.packet.Packet;

public
class PacketSyntaxCheckLink extends AbstractPacketProcessorLink {

    private static final String SYNTAX_ERROR_STRING;
    private final PacketCheck packetValidator;

    static {
        SYNTAX_ERROR_STRING = "Paket aufgrund eines Syntaxfehlers nicht bearbeitet.";
    }

    public
    PacketSyntaxCheckLink (PacketProcessor nextStep, PacketCheck packetValidator) {
        super(nextStep);
        this.packetValidator = packetValidator;
    }

    @Override
    public
    void processPacket (Packet input)
    throws PacketValidationException, PacketProcessingException {
        var syntaxCheckString = this.packetValidator.checkPacket(input);

        if (syntaxCheckString != null) {
            throw new PacketValidationException(
                    SYNTAX_ERROR_STRING + syntaxCheckString);
        }
        super.nextLink.processPacket(input);
    }
}
