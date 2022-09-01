/*
 *
 */
package de.vsy.server.service.packet_logic.processor;

import de.vsy.server.exception_processing.ServerPacketHandlingExceptionCreator;
import de.vsy.server.service.packet_logic.PacketResponseMap;
import de.vsy.server.service.packet_logic.ServicePacketProcessorFactory;
import de.vsy.shared_module.shared_module.exception_processing.PacketHandlingExceptionProcessor;
import de.vsy.shared_module.shared_module.packet_exception.PacketProcessingException;
import de.vsy.shared_transmission.shared_transmission.packet.Packet;

/**
 * Basic Packetprocessor using the strategy that is passed through the constructor.
 */
public
class ServicePacketProcessor {

    private final PacketHandlingExceptionProcessor pheProcessor;
    /**
     * Servicespezifische Fabrik zur Bereitstellung von
     * Paketverarbeitungsmechanismen.
     */
    private final ServicePacketProcessorFactory sphf;

    /**
     * Instantiates a new service Packetthis.processor.
     *
     * @param sphf the sphf
     */
    public
    ServicePacketProcessor (final ServicePacketProcessorFactory sphf) {
        super();
        this.sphf = sphf;
        this.pheProcessor = ServerPacketHandlingExceptionCreator.getServiceExceptionProcessor();
    }

    /**
     * Process Packet
     *
     * @param input the input
     *
     * @return the processor Packetresponse map
     */
    public
    PacketResponseMap processPacket (final Packet input) {
        final var responses = new PacketResponseMap();

        try {
            final var identifier = input.getPacketProperties()
                                        .getPacketIdentificationProvider();
            final var packetType = identifier.getPacketType();
            final var ph = this.sphf.getPacketProcessor(packetType);

            if (ph != null) {
                return ph.processPacket(input);
            }
            final var errorMessage = "Paket wurde nicht verarbeitet.";
            final var errorCause = "Paket-Identifier oder -Typ nicht gefunden.";

            throw new PacketProcessingException(errorMessage + errorCause);
        } catch (final PacketProcessingException phe) {
            responses.setClientBoundPacket(
                    this.pheProcessor.processException(phe, input));
        }

        return responses;
    }
}
