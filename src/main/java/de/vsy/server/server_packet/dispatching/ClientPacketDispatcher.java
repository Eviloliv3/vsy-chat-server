package de.vsy.server.server_packet.dispatching;

import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_CLIENT_ID;

import de.vsy.shared_module.packet_management.BasicClientPacketDispatcher;
import de.vsy.shared_module.packet_management.ClientDataProvider;
import de.vsy.shared_module.packet_management.PacketBuffer;

public class ClientPacketDispatcher extends BasicClientPacketDispatcher {

  private final ClientDataProvider clientData;

  public ClientPacketDispatcher(final ClientDataProvider clientData,
      final PacketBuffer clientBoundBuffer, final PacketBuffer serverBoundBuffer) {
    super(clientBoundBuffer, serverBoundBuffer);
    this.clientData = clientData;
  }

  @Override
  protected boolean isClientBound(int recipientId) {
    final int clientId = this.clientData.getClientId();
    return clientId == STANDARD_CLIENT_ID || recipientId == clientId
        || recipientId == STANDARD_CLIENT_ID;
  }
}
