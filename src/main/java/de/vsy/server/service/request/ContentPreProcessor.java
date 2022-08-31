package de.vsy.server.service.request;

import de.vsy.server.server.data.AbstractPacketCategorySubscriptionManager;
import de.vsy.server.server.server_connection.LocalServerConnectionData;
import de.vsy.server.server_packet.content.ExtendedStatusSyncDTO;
import de.vsy.shared_module.shared_module.packet_creation.PacketCompiler;
import de.vsy.shared_module.shared_module.packet_exception.PacketProcessingException;
import de.vsy.shared_module.shared_module.packet_management.OutputBuffer;
import de.vsy.shared_transmission.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.shared_transmission.packet.content.PacketContent;

public
class ContentPreProcessor implements PublishablePacketCreator {

    private final LocalServerConnectionData localServerData;
    private final ExtendedClientStatusPreProcessor extendedStatusProcessor;

    public
    ContentPreProcessor (final LocalServerConnectionData localServerData,
                         AbstractPacketCategorySubscriptionManager clientSubscriptions,
                         OutputBuffer assignmentBuffer) {
        this.localServerData = localServerData;
        extendedStatusProcessor = new ExtendedClientStatusPreProcessor(
                clientSubscriptions, assignmentBuffer);
    }

    @Override
    public
    Packet handleDistributableContent (Packet input)
    throws PacketProcessingException {
        Packet processedPacket = null;
        if (localServerRecipient(input) &&
            input.getPacketContent() instanceof ExtendedStatusSyncDTO statusContent) {
            final PacketContent newContent = extendedStatusProcessor.processContent(
                    statusContent);

            if (newContent != null) {
                processedPacket = PacketCompiler.createRequest(
                        input.getPacketProperties().getRecipientEntity(),
                        newContent);
            }
        } else {
            processedPacket = input;
        }
        return processedPacket;
    }

    private
    boolean localServerRecipient (final Packet input) {
        return input.getPacketProperties().getRecipientEntity().getEntityId() ==
               this.localServerData.getServerId();
    }
}
