package de.vsy.server.client_handling.packet_processing.processor;

import de.vsy.server.client_handling.data_management.bean.LocalClientDataProvider;
import de.vsy.server.server.data.access.HandlerAccessManager;
import de.vsy.server.server_packet.content.SimpleInternalContentWrapper;
import de.vsy.server.server_packet.content.builder.SimpleInternalContentBuilder;
import de.vsy.shared_module.shared_module.packet_creation.PacketCompiler;
import de.vsy.shared_transmission.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.shared_transmission.packet.content.PacketContent;
import de.vsy.shared_transmission.shared_transmission.packet.property.communicator.CommunicationEndpoint;
import de.vsy.shared_utility.standard_value.StandardIdProvider;

public abstract
class ResultingPacketCreator {
    protected Packet currentRequest;

    protected
    ResultingPacketCreator () {
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
        final var recipient = this.currentRequest.getPacketProperties()
                                                 .getRecipient();
        return createRequest(processedContent, recipient);
    }

    public abstract
    Packet createRequest(PacketContent processedContent, final CommunicationEndpoint recipient);

    public abstract
    Packet createResponse (PacketContent processedContent);

    protected
    PacketContent wrapContent (PacketContent processedContent) {
        final var newWrapper = new SimpleInternalContentBuilder();
        final var serverId = HandlerAccessManager.getLocalServerConnectionData()
                                                 .getServerId();
        var initialContent = processedContent;

        if (processedContent instanceof final SimpleInternalContentWrapper wrappedContent) {
            initialContent = wrappedContent.getWrappedContent();
            newWrapper.withSynchronizedServers(wrappedContent.getSynchronizedServers());
        }
        newWrapper.withContent(initialContent).withOriginatingServerId(serverId);
        return newWrapper.build();
    }
}
