package de.vsy.server.client_handling.packet_processing.content_processor_provisioning;

import de.vsy.server.client_handling.data_management.HandlerLocalDataManager;
import de.vsy.shared_module.packet_processing.processor_provision.ContentBasedProcessorFactory;
import de.vsy.shared_transmission.packet.property.packet_category.PacketCategory;

public class StandardProcessorFactoryProvider implements CategoryBasedProcessorFactoryProvider {

  @Override
  public ContentBasedProcessorFactory getCategoryHandlerFactory(PacketCategory category,
      HandlerLocalDataManager threadDataAccess) {
    ContentBasedProcessorFactory categoryFactory = null;

    switch (category) {
      case AUTHENTICATION ->
          categoryFactory = new AuthenticationPacketProcessorFactory(threadDataAccess);
      case CHAT -> categoryFactory = new ChatPacketProcessorFactory(threadDataAccess);
      case STATUS -> categoryFactory = new StatusPacketProcessorFactory(threadDataAccess);
      case RELATION -> categoryFactory = new RelationPacketProcessorFactory(threadDataAccess);
      case ERROR -> categoryFactory = new ErrorPacketProcessorFactory(threadDataAccess);
      default -> {
      }
      // fehler loggen
    }
    return categoryFactory;
  }
}
