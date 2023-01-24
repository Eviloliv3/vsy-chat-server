package de.vsy.server.server_packet.packet_creation;

import de.vsy.server.client_handling.packet_processing.processor.ResultingPacketCreator;
import de.vsy.shared_module.packet_creation.PacketCompiler;
import de.vsy.shared_module.packet_management.ClientDataProvider;
import de.vsy.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.packet.content.PacketContent;
import de.vsy.shared_transmission.packet.property.communicator.CommunicationEndpoint;
import org.apache.logging.log4j.LogManager;

import static de.vsy.shared_transmission.packet.property.communicator.EligibleCommunicationEntity.CLIENT;
import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_CLIENT_ID;

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

        final var resultingContent = adjustWrapping(processedContent, recipient);
        if (super.currentRequest == null) {
            return PacketCompiler.createRequest(recipient, resultingContent);
        } else {
            return PacketCompiler.createFollowUpRequest(recipient, resultingContent, this.currentRequest);
        }
    }

    @Override
    public Packet createResponse(PacketContent processedContent) {
        final var recipient = super.currentRequest.getPacketProperties().getSender();
        final var resultingContent = adjustWrapping(processedContent, recipient);
        return PacketCompiler.createResponse(resultingContent, this.currentRequest);
    }

    protected PacketContent adjustWrapping(final PacketContent contentToWrap,
                                           final CommunicationEndpoint recipient) {
        PacketContent finalContent;
        boolean toWrap = true;
        final boolean recipientIsClient = recipient.getEntity().equals(CLIENT);

        if (recipientIsClient) {
            LogManager.getLogger().error("For: {}", contentToWrap);
            toWrap = !(localClientIsRecipient(recipient.getEntityId()));
        }

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
    protected boolean localClientIsRecipient(final int recipientId) {
        final int localClientId = this.clientDataProvider.getClientId();
        final boolean noLocalClient = localClientId == STANDARD_CLIENT_ID;
        final boolean localClientIsRecipient = localClientId == recipientId;
        final var senderIsUnspecifiedClient = recipientId == STANDARD_CLIENT_ID;
        LogManager.getLogger().error("noLocal: {} | localRec: {} | unspecSender: {} ", noLocalClient, localClientIsRecipient, senderIsUnspecifiedClient);

        return noLocalClient || localClientIsRecipient || senderIsUnspecifiedClient;
    }
}
