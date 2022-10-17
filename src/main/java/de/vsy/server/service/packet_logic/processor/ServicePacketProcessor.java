/*
 *
 */
package de.vsy.server.service.packet_logic.processor;

import de.vsy.server.server_packet.packet_creation.ResultingPacketContentHandler;
import de.vsy.server.service.packet_logic.ServicePacketProcessorFactory;
import de.vsy.shared_module.shared_module.packet_exception.PacketHandlingException;
import de.vsy.shared_transmission.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.shared_transmission.packet.content.error.ErrorDTO;

/**
 * Basic Packetprocessor using the strategy that is passed through the constructor.
 */
public class ServicePacketProcessor {

  /**
   * Servicespezifische Fabrik zur Bereitstellung von Paketverarbeitungsmechanismen.
   */
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
        final var errorContent = new ErrorDTO(phe.getMessage(), input);
        this.contentHandler.setError(errorContent);
      }
    } else {
      final var errorMessage = "Paket wurde nicht verarbeitet. Paket-"
          + "Identifzierer oder -Typ nicht gefunden.";
      final var errorContent = new ErrorDTO(errorMessage, input);
      this.contentHandler.setError(errorContent);
    }
  }
}
