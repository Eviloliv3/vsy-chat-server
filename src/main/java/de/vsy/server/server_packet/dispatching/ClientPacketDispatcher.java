/*
 *
 */
package de.vsy.server.server_packet.dispatching;

import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_CLIENT_ID;

import de.vsy.server.client_handling.data_management.bean.LocalClientDataProvider;
import de.vsy.shared_module.packet_management.MultiplePacketDispatcher;
import de.vsy.shared_module.packet_management.PacketBuffer;
import de.vsy.shared_module.packet_management.ThreadPacketBufferLabel;
import de.vsy.shared_module.packet_management.ThreadPacketBufferManager;
import de.vsy.shared_transmission.packet.Packet;
import java.util.Deque;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClientPacketDispatcher implements MultiplePacketDispatcher {

  private static final Logger LOGGER = LogManager.getLogger();
  private final LocalClientDataProvider clientDataManager;
  private final PacketBuffer serverBoundBuffer;
  private final PacketBuffer clientBoundBuffer;

  /**
   * Instantiates a new handler Packetdispatcher.
   *
   * @param threadLocalBuffers the thread local buffers
   */
  public ClientPacketDispatcher(final LocalClientDataProvider clientDataManager,
      final ThreadPacketBufferManager threadLocalBuffers) {
    this.clientDataManager = clientDataManager;
    this.clientBoundBuffer = threadLocalBuffers.getPacketBuffer(
        ThreadPacketBufferLabel.OUTSIDE_BOUND);
    this.serverBoundBuffer = threadLocalBuffers.getPacketBuffer(
        ThreadPacketBufferLabel.SERVER_BOUND);
  }

  @Override
  public void dispatchPacket(final Deque<Packet> output) {

    while (!output.isEmpty()) {
      final Packet toDispatch = output.pop();
      dispatchPacket(toDispatch);
    }
  }

  @Override
  public void dispatchPacket(final Packet toAppend) {
    if (toAppend == null) {
      throw new IllegalArgumentException("Leeres Paket wird nicht gepuffert.");
    }
    final var recipient = toAppend.getPacketProperties().getRecipient();
    final var recipientId = recipient.getEntityId();

    if (this.isClientBound(recipientId)) {
      this.clientBoundBuffer.appendPacket(toAppend);
    } else {
      this.serverBoundBuffer.appendPacket(toAppend);
    }
  }

  /**
   * Packet is client bound if: local client not authenticated; recipient is STANDARD_CLIENT_ID
   * recipientId equals clientId
   *
   * @param recipientId the recipient id
   * @return true if client is recipient, else false
   */
  private boolean isClientBound(final int recipientId) {
    final int clientId = this.clientDataManager.getClientId();
    return clientId == STANDARD_CLIENT_ID || recipientId == clientId
        || recipientId == STANDARD_CLIENT_ID;
  }
}
