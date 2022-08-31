package de.vsy.server.client_handling.packet_processing.processor;

import de.vsy.server.client_handling.data_management.bean.LocalClientDataProvider;
import de.vsy.server.server.data.access.HandlerAccessManager;
import de.vsy.server.server_packet.content.SimpleInternalContentWrapper;
import de.vsy.server.server_packet.content.builder.SimpleInternalContentBuilder;
import de.vsy.shared_module.shared_module.packet_creation.PacketCompiler;
import de.vsy.shared_transmission.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.shared_transmission.packet.content.PacketContent;
import de.vsy.shared_utility.standard_value.StandardIdProvider;

public
class ResultingPacketCreator {

    private final LocalClientDataProvider clientDataProvider;
    private Packet currentRequest;

    public
    ResultingPacketCreator (LocalClientDataProvider clientDataProvider) {
        this.clientDataProvider = clientDataProvider;
    }

    /**
     * Sets the committed packet as the current this.currentRequest.
     *
     * @param currentRequest the this.currentRequest, responses/results will
     *         be base on
     */
    public
    void changeCurrentRequest (Packet currentRequest) {
        this.currentRequest = currentRequest;
    }

    public
    Packet createRequest (PacketContent processedContent) {
        checkCurrentRequest();

        if (processedContent == null) {
            throw new IllegalArgumentException("Kein Paketinhalt uebergeben.");
        }
        final var resultingContent = adjustWrapping(processedContent, true);
        final var recipient = this.currentRequest.getPacketProperties()
                                                 .getRecipientEntity();
        return PacketCompiler.createFollowUpRequest(recipient, resultingContent,
                                                    this.currentRequest);
    }

    private
    void checkCurrentRequest ()
    throws IllegalStateException {
        if (this.currentRequest == null) {
            throw new IllegalStateException(
                    "Kein zu beantwortendes Paket angegeben.");
        }
    }

    private
    PacketContent adjustWrapping (final PacketContent contentToWrap,
                                  final boolean isRequest) {
        PacketContent finalContent;

        final var clientIsSender = checkClientSender();
        final var toWrap =
                (!isRequest && !clientIsSender) || (isRequest && clientIsSender);

        if (toWrap) {
            finalContent = wrapContent(contentToWrap);
        } else {
            finalContent = contentToWrap;
        }

        return finalContent;
    }

    private
    boolean checkClientSender () {
        final var senderId = this.currentRequest.getPacketProperties()
                                                .getSenderEntity()
                                                .getEntityId();
        return senderId == this.clientDataProvider.getClientId() ||
               senderId == StandardIdProvider.STANDARD_CLIENT_ID;
    }

    private
    PacketContent wrapContent (PacketContent processedContent) {
        final var newWrapper = new SimpleInternalContentBuilder();
        final var serverId = HandlerAccessManager.getLocalServerConnectionData()
                                                 .getServerId();
        var initialContent = processedContent;

        if (processedContent instanceof final SimpleInternalContentWrapper wrappedContent) {
            initialContent = wrappedContent.getWrappedContent();
            newWrapper.withSyncedServers(wrappedContent.getSyncedServers());
        }
        newWrapper.withContent(initialContent).withOriginatingServerId(serverId);
        return newWrapper.build();
    }

    public
    Packet createResponse (PacketContent processedContent) {
        checkCurrentRequest();
        final var resultingContent = adjustWrapping(processedContent, false);
        return PacketCompiler.createResponse(resultingContent, this.currentRequest);
    }
}
