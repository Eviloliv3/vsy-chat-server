package de.vsy.chat.server.service.request;

import de.vsy.chat.shared_module.packet_exception.PacketProcessingException;
import de.vsy.chat.shared_module.packet_management.OutputBuffer;
import de.vsy.chat.server.server.data.AbstractPacketCategorySubscriptionManager;
import de.vsy.chat.server.server_packet.content.ExtendedStatusSyncDTO;
import de.vsy.chat.server.service.status_synchronization.PacketDemultiplexer;
import de.vsy.chat.shared_transmission.packet.Packet;
import de.vsy.chat.shared_transmission.packet.content.PacketContent;

import java.util.HashSet;
import java.util.Set;

import static de.vsy.chat.shared_transmission.packet.property.packet_category.PacketCategory.CHAT;

public
class ExtendedClientStatusPreProcessor {

    private final OutputBuffer assignmentBuffer;
    private final AbstractPacketCategorySubscriptionManager clientSubscriptions;

    public
    ExtendedClientStatusPreProcessor (
            AbstractPacketCategorySubscriptionManager clientSubscriptions,
            OutputBuffer assignmentBuffer) {
        this.assignmentBuffer = assignmentBuffer;
        this.clientSubscriptions = clientSubscriptions;
    }

    public
    PacketContent processContent (ExtendedStatusSyncDTO toProcess)
    throws PacketProcessingException {
        Set<Packet> updatePackets;
        Set<Integer> eligibleRecipients = new HashSet<>(
                toProcess.getContactIdList());

        eligibleRecipients = clientSubscriptions.validateThreadIds(CHAT,
                                                                   eligibleRecipients);
        updatePackets = PacketDemultiplexer.demultiplexPacket(toProcess,
                                                              eligibleRecipients);

        if (!updatePackets.isEmpty()) {
            for (final Packet updatePacket : updatePackets) {
                assignmentBuffer.prependPacket(updatePacket);
                eligibleRecipients.remove(updatePacket.getPacketProperties()
                                                      .getRecipientEntity()
                                                      .getEntityId());
            }
        }
        return toProcess.getContactIdList().isEmpty() ? null : toProcess;
    }
}
