package de.vsy.server.server_packet.dispatching;

import de.vsy.server.data.socketConnection.RemoteServerConnectionData;
import de.vsy.server.service.Service;
import de.vsy.server.service.ServiceData;
import de.vsy.server.service.ServicePacketBufferManager;
import de.vsy.shared_module.shared_module.packet_management.OutputBuffer;
import de.vsy.shared_module.shared_module.packet_management.PacketBuffer;
import de.vsy.shared_transmission.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.shared_transmission.packet.property.communicator.EligibleCommunicationEntity;
import java.util.Deque;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class InterServerCommunicationPacketDispatcher implements MultiplePacketDispatcher {

  private static final Logger LOGGER = LogManager.getLogger();
  private final RemoteServerConnectionData remoteServerData;
  private final ServicePacketBufferManager serviceBuffers;
  private final Map<ServiceData.ServiceResponseDirection, Service.TYPE> responseDirections;
  private final OutputBuffer outputBuffer;

  public InterServerCommunicationPacketDispatcher(final RemoteServerConnectionData remoteServerData,
      final ServicePacketBufferManager serviceBuffers,
      final Map<ServiceData.ServiceResponseDirection, Service.TYPE> responseDirections,
      final OutputBuffer outputBuffer) {
    this.remoteServerData = remoteServerData;
    this.serviceBuffers = serviceBuffers;
    this.responseDirections = responseDirections;
    this.outputBuffer = outputBuffer;
  }

  @Override
  public void dispatchPacket(Deque<Packet> output) {
    while (!output.isEmpty()) {
      final Packet toDispatch = output.pop();
      dispatchPacket(toDispatch);
    }
  }

  /**
   * @param output the packet to dispatch
   * @throws IllegalArgumentException wenn Paket null, Properties null, oder Empfänger null,
   *                                  EmpfängerEntität null
   */
  @Override
  public void dispatchPacket(final Packet output) {
    if (serverIsRecipient(output)) {
      sendInboundPacket(output);
    } else {
      sendOutboundPacket(output);
    }
  }

  private boolean serverIsRecipient(final Packet output) {
    final var recipientId = output.getPacketProperties().getRecipient().getEntityId();
    return recipientId != remoteServerData.getServerId()
        && recipientId != remoteServerData.getServerPort();
  }

  /**
   * Regelt den Versand eines, an den Klienten gerichteten Paketes.
   *
   * @param output Das Paket vom Typ Packet dass versandt wird.
   */
  protected void sendInboundPacket(final Packet output) {
    if (output == null) {
      throw new IllegalArgumentException("Leeres Paket wird nicht gepuffert.");
    }
    PacketBuffer buffer;

    buffer = this.serviceBuffers
        .getRandomBuffer(this.responseDirections.get(ServiceData.ServiceResponseDirection.INBOUND));

    if (buffer != null) {
      buffer.appendPacket(output);
    }
  }

  /**
   * Regelt den Versand eines, an den Server gerichteten, Paketes.
   *
   * @param output Das Paket vom Typ Packet dass versandt wird.
   */
  protected void sendOutboundPacket(final Packet output) {
    outputBuffer.appendPacket(output);
  }

  private EligibleCommunicationEntity getRecipientEntity(Packet output) {
    EligibleCommunicationEntity recipientEntity;
    if (output != null) {
      final var recipient = output.getPacketProperties().getRecipient();

      if (recipient != null) {
        recipientEntity = recipient.getEntity();
      } else {
        throw new IllegalArgumentException("Kein Empfänger angegeben.");
      }
    } else {
      throw new IllegalArgumentException("Kein Paket zum Versenden übergeben.");
    }
    return recipientEntity;
  }
}
