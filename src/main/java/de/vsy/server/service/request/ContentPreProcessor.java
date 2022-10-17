package de.vsy.server.service.request;

import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_CLIENT_BROADCAST_ID;

import de.vsy.server.server.data.AbstractPacketCategorySubscriptionManager;
import de.vsy.server.server.server_connection.LocalServerConnectionData;
import de.vsy.server.server_packet.content.ExtendedStatusSyncDTO;
import de.vsy.shared_module.shared_module.packet_exception.PacketProcessingException;
import de.vsy.shared_module.shared_module.packet_management.OutputBuffer;
import de.vsy.shared_transmission.shared_transmission.packet.Packet;
import org.apache.logging.log4j.LogManager;

public class ContentPreProcessor implements PublishablePacketCreator {

  private final LocalServerConnectionData localServerData;
  private final ExtendedClientStatusPreProcessor extendedStatusProcessor;

  public ContentPreProcessor(final LocalServerConnectionData localServerData,
      AbstractPacketCategorySubscriptionManager clientSubscriptions,
      OutputBuffer assignmentBuffer) {
    this.localServerData = localServerData;
    extendedStatusProcessor = new ExtendedClientStatusPreProcessor(clientSubscriptions,
        assignmentBuffer);
  }

  @Override
  public Packet handleDistributableContent(Packet input) throws PacketProcessingException {
    Packet processedPacket = null;

    if (isLocalBroadcast(input)
        && input.getPacketContent() instanceof ExtendedStatusSyncDTO statusContent) {
      LogManager.getLogger().debug("ExtendedStatusSync gelesen");
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
