package de.vsy.server.server_packet.packet_creation;

import de.vsy.server.client_handling.packet_processing.processor.ResultingPacketCreator;
import de.vsy.shared_module.packet_creation.PacketCompiler;
import de.vsy.shared_module.packet_management.ClientDataProvider;
import de.vsy.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.packet.content.PacketContent;
import de.vsy.shared_transmission.packet.property.communicator.CommunicationEndpoint;
import de.vsy.shared_utility.standard_value.StandardIdProvider;

public class ClientHandlerPacketCreator extends ResultingPacketCreator {

    private final ClientDataProvider clientDataProvider;

    public ClientHandlerPacketCreator(ClientDataProvider clientDataProvider) {
        this.clientDataProvider = clientDataProvider;
    }

    @Override
    public Packet createRequest(PacketContent processedContent, CommunicationEndpoint recipient) {
        if (processedContent == null) {
            throw new IllegalArgumentException("No PacketContent specified.");
        }
        if (recipient == null) {
            throw new IllegalArgumentException("No recipient specified.");
        }

        final var resultingContent = adjustWrapping(processedContent, true);
        if (this.currentRequest == null) {
            return PacketCompiler.createRequest(recipient, resultingContent);
        } else {
            return PacketCompiler.createFollowUpRequest(recipient, resultingContent, this.currentRequest);
        }
    }

    @Override
    public Packet createResponse(PacketContent processedContent) {
        final var resultingContent = adjustWrapping(processedContent, false);
        return PacketCompiler.createResponse(resultingContent, this.currentRequest);
    }

    protected PacketContent adjustWrapping(final PacketContent contentToWrap,
                                           final boolean isRequest) {
        PacketContent finalContent;

        final var clientIsSender = checkClientSender();
        final var toWrap = (!isRequest && !clientIsSender) || (isRequest && clientIsSender);

        if (toWrap) {
            finalContent = wrapContent(contentToWrap);
        } else {
            finalContent = contentToWrap;
        }

        return finalContent;
    }

    /**
     * Checks whether current request came from connected client. Three distinguished cases: local
     * client id equals packet sender id; sender id is standard client id -> client generally does not
     * have id before authentication and no standard client id packet is sent through server
     * infrastructure; local client id is standard client id -> no client authentication data,
     * therefore client handler is not registered in server's packet forwarding network and the
     * request has to have been sent from the connected client
     *
     * @return true if the connected client is the originator
     */
    protected boolean checkClientSender() {
        final var senderId = this.currentRequest.getPacketProperties().getSender().getEntityId();
        final var localClientId = this.clientDataProvider.getClientId();
        final var noClientAuthenticated = localClientId == StandardIdProvider.STANDARD_CLIENT_ID;
        return noClientAuthenticated
                || senderId == this.clientDataProvider.getClientId()
                || senderId == StandardIdProvider.STANDARD_CLIENT_ID;
    }
}
