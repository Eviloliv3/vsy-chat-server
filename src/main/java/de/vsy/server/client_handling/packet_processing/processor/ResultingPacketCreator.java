package de.vsy.server.client_handling.packet_processing.processor;

import de.vsy.server.data.access.HandlerAccessManager;
import de.vsy.server.server_packet.content.SimpleInternalContentWrapper;
import de.vsy.server.server_packet.content.builder.SimpleInternalContentBuilder;
import de.vsy.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.packet.content.PacketContent;
import de.vsy.shared_transmission.packet.property.communicator.CommunicationEndpoint;

public abstract class ResultingPacketCreator {

    protected Packet currentRequest;

    protected ResultingPacketCreator() {
    }

    /**
     * Sets the committed packet as the current currentRequest.
     *
     * @param currentRequest the this.currentRequest, responses/results will be base on
     */
    public void setCurrentPacket(Packet currentRequest) {
        this.currentRequest = currentRequest;
    }

    public Packet createRequest(PacketContent processedContent) {
        final var recipient = this.currentRequest.getPacketProperties().getRecipient();
        return createRequest(processedContent, recipient);
    }

    /**
     * Creates a Packet directed at a specified recipient.
     * @param processedContent  the request PacketContent
     * @param recipient         the custom recipient
     * @return                  a request Packet
     */
    public abstract Packet createRequest(PacketContent processedContent,
                                         final CommunicationEndpoint recipient);

    /**
     * Creates a Packet that is directed at the sender of the original packet.
     * @param processedContent the response PacketContent
     * @return a response Packet
     */
    public abstract Packet createResponse(PacketContent processedContent);

    /**
     * Creates a SimpleInternalContentWrapper containing the specified PacketContent.
     * @param processedContent  the PacketContent to wrap
     * @return SimpleInternalContentWrapper containing the specified PacketContent
     */
    protected PacketContent wrapContent(PacketContent processedContent) {
        final var newWrapper = new SimpleInternalContentBuilder();
        final var serverId = HandlerAccessManager.getLocalServerConnectionData().getServerId();
        var initialContent = processedContent;

        if (processedContent instanceof final SimpleInternalContentWrapper wrappedContent) {
            initialContent = wrappedContent.getWrappedContent();
            newWrapper.withSynchronizedServers(wrappedContent.getSynchronizedServers());
        }
        newWrapper.withContent(initialContent).withOriginatingServerId(serverId);
        return newWrapper.build();
    }
}
