package de.vsy.server.service.request;

import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_CLIENT_BROADCAST_ID;

import de.vsy.server.data.AbstractPacketCategorySubscriptionManager;
import de.vsy.server.data.socketConnection.LocalServerConnectionData;
import de.vsy.server.server_packet.content.ExtendedStatusSyncDTO;
import de.vsy.shared_module.packet_management.OutputBuffer;
import de.vsy.shared_transmission.packet.Packet;
import org.apache.logging.log4j.LogManager;

public class ContentPreProcessor implements PublishablePacketCreator {

  private final ExtendedClientStatusPreProcessor extendedStatusProcessor;

  public ContentPreProcessor(final LocalServerConnectionData localServerData,
      AbstractPacketCategorySubscriptionManager clientSubscriptions,
      OutputBuffer assignmentBuffer) {
    extendedStatusProcessor = new ExtendedClientStatusPreProcessor(clientSubscriptions,
        assignmentBuffer);
  }

  @Override
  public Packet handleDistributableContent(Packet input) {
    Packet processedPacket = null;

    if (isLocalBroadcast(input)
        && input.getPacketContent() instanceof ExtendedStatusSyncDTO statusContent) {
      LogManager.getLogger().trace("ExtendedStatusSync gelesen");
      extendedStatusProcessor.processContent(statusContent);
    } else {
      processedPacket = input;
    }
    return processedPacket;
  }

  private boolean isLocalBroadcast(final Packet input) {
    return input.getPacketProperties().getRecipient().getEntityId() == STANDARD_CLIENT_BROADCAST_ID;
  }
}
