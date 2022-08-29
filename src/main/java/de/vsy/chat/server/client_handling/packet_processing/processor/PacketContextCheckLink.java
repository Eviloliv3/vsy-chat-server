package de.vsy.chat.server.client_handling.packet_processing.processor;

import de.vsy.chat.shared_module.packet_exception.PacketProcessingException;
import de.vsy.chat.shared_module.packet_exception.PacketValidationException;
import de.vsy.chat.shared_module.packet_processing.PacketProcessor;
import de.vsy.chat.server.client_handling.packet_processing.request_filter.PermittedPacketCategoryCheck;
import de.vsy.chat.shared_transmission.packet.Packet;

public
class PacketContextCheckLink extends AbstractPacketProcessorLink {

    private final PermittedPacketCategoryCheck packetCategoryCheck;

    public
    PacketContextCheckLink (PacketProcessor nextStep,
                            PermittedPacketCategoryCheck packetCategoryCheck) {
        super(nextStep);
        this.packetCategoryCheck = packetCategoryCheck;
    }

    @Override
    public
    void processPacket (Packet input)
    throws PacketValidationException, PacketProcessingException {
        var inputCategory = input.getPacketProperties()
                                 .getContentIdentifier()
                                 .getPacketCategory();

        if (!this.packetCategoryCheck.checkPacketCategory(inputCategory)) {
            var errorCause = "Pakete der Kategorie \"" + inputCategory +
                             "\" dürfen im aktuellen Zustand nicht verarbeitet werden." +
                             " Zulaessige Kategorien sind: " +
                             this.packetCategoryCheck.getPermittedPacketCategories();

            throw new PacketValidationException(errorCause);
        }
        super.nextLink.processPacket(input);
    }
}
