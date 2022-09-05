/*
 *
 */
package de.vsy.server.service.packet_logic.processor;

import de.vsy.server.exception_processing.ServerPacketHandlingExceptionCreator;
import de.vsy.server.service.packet_logic.PacketResponseMap;
import de.vsy.server.service.packet_logic.ServicePacketProcessorFactory;
import de.vsy.shared_module.shared_module.exception_processing.PacketHandlingExceptionProcessor;
import de.vsy.shared_module.shared_module.packet_exception.PacketHandlingException;
import de.vsy.shared_module.shared_module.packet_exception.PacketProcessingException;
import de.vsy.shared_transmission.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.shared_transmission.packet.property.communicator.EligibleCommunicationEntity;

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
     * Instantiates a new service packet processor.
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
     * @return the processor packet response map
     */
    public
    PacketResponseMap processPacket (final Packet input) {
        var responses = new PacketResponseMap();
        final var identifier = input.getPacketProperties()
                                    .getPacketIdentificationProvider();
        final var packetType = identifier.getPacketType();
        final var ph = this.sphf.getPacketProcessor(packetType);

        if (ph != null) {
            try {
                responses = ph.processPacket(input);
            } catch (final PacketProcessingException phe) {
                handleException(phe, input, responses);
            }
        }else{
            final var errorMessage = "Paket wurde nicht verarbeitet. Paket-" +
                                     "Identifzierer oder -Typ nicht gefunden.";
            final var processingException = new PacketProcessingException(errorMessage);
            handleException(processingException, input, responses);
        }

        return responses;
    }

    private void handleException(final PacketHandlingException phe, final Packet input, PacketResponseMap responseMap){
        final var errorResponse = this.pheProcessor.processException(phe, input);

        if(errorResponse != null) {
            if (errorResponse.getPacketProperties()
                             .getRecipient()
                             .getEntity()
                             .equals(EligibleCommunicationEntity.SERVER)) {
                responseMap.setServerBoundPacket(errorResponse);
            }else{
                responseMap.setClientBoundPacket(errorResponse);
            }
        }
    }
}
