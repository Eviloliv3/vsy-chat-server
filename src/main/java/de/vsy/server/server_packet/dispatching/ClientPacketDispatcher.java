package de.vsy.server.server_packet.dispatching;

import de.vsy.shared_module.packet_management.BasicClientPacketDispatcher;
import de.vsy.shared_module.packet_management.ClientDataProvider;
import de.vsy.shared_module.packet_management.PacketBuffer;
import de.vsy.shared_transmission.packet.property.communicator.CommunicationEndpoint;

import static de.vsy.shared_transmission.packet.property.communicator.EligibleCommunicationEntity.CLIENT;
import static de.vsy.shared_transmission.packet.property.communicator.EligibleCommunicationEntity.SERVER;
import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_CLIENT_ID;

public class ClientPacketDispatcher extends BasicClientPacketDispatcher {

    private final ClientDataProvider clientData;

    public ClientPacketDispatcher(final ClientDataProvider clientData,
                                  final PacketBuffer clientBoundBuffer, final PacketBuffer serverBoundBuffer) {
        super(clientBoundBuffer, serverBoundBuffer);
        this.clientData = clientData;
    }

    @Override
    protected boolean clientIsRecipient(final CommunicationEndpoint sender, final CommunicationEndpoint recipient) {
        final int clientId = this.clientData.getClientId();
        final var recipientId = recipient.getEntityId();
        final var recipientTypeIsClient = recipient.getEntity().equals(CLIENT);
        final var senderTypeIsServer = sender.getEntity().equals(SERVER);
        final var noLocalClient = clientId == STANDARD_CLIENT_ID;
        final var localClientIsRecipient = clientId == recipientId;
        final var recipientIsUnspecified = recipientId == STANDARD_CLIENT_ID;

        return recipientTypeIsClient && (localClientIsRecipient ||
                (senderTypeIsServer && (noLocalClient || recipientIsUnspecified)));
    }
}
