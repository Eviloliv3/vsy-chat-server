package de.vsy.server.service.request;

import static de.vsy.shared_transmission.shared_transmission.packet.property.packet_category.PacketCategory.CHAT;

import de.vsy.server.data.AbstractPacketCategorySubscriptionManager;
import de.vsy.server.server_packet.content.ExtendedStatusSyncDTO;
import de.vsy.server.service.status_synchronization.PacketDemultiplexer;
import de.vsy.shared_module.shared_module.packet_management.OutputBuffer;
import de.vsy.shared_transmission.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.shared_transmission.packet.content.PacketContent;
import java.util.HashSet;
import java.util.Set;

public class ExtendedClientStatusPreProcessor {

  private final OutputBuffer assignmentBuffer;
  private final AbstractPacketCategorySubscriptionManager clientSubscriptions;

  public ExtendedClientStatusPreProcessor(
      AbstractPacketCategorySubscriptionManager clientSubscriptions,
      OutputBuffer assignmentBuffer) {
    this.assignmentBuffer = assignmentBuffer;
    this.clientSubscriptions = clientSubscriptions;
  }

  public PacketContent processContent(ExtendedStatusSyncDTO toProcess) {
    Set<Packet> updatePackets;
    Set<Integer> eligibleRecipients = new HashSet<>(toProcess.getContactIdSet());

    eligibleRecipients = clientSubscriptions.getLocalThreads(CHAT, eligibleRecipients);

    updatePackets = PacketDemultiplexer.demultiplexPacket(toProcess, eligibleRecipients);

    if (!updatePackets.isEmpty()) {
      for (final Packet updatePacket : updatePackets) {
        assignmentBuffer.prependPacket(updatePacket);
        eligibleRecipients.remove(updatePacket.getPacketProperties().getRecipient().getEntityId());
      }
    }
    return toProcess.getContactIdSet().isEmpty() ? null : toProcess;
  }
}
