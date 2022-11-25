/*
 *
 */
package de.vsy.server.client_handling.packet_processing.content_processor_provisioning;

import static de.vsy.shared_transmission.packet.content.chat.ChatContent.TextMessageDTO;

import de.vsy.server.client_handling.data_management.HandlerLocalDataManager;
import de.vsy.server.client_handling.packet_processing.ClientHandlerPacketProcessor;
import de.vsy.server.client_handling.packet_processing.content_processing.TextMessageProcessor;
import de.vsy.shared_module.packet_processing.PacketProcessor;
import de.vsy.shared_module.packet_processing.ProcessingConditionType;
import de.vsy.shared_module.packet_processing.processor_provision.ContentBasedProcessorFactory;
import de.vsy.shared_module.packet_validation.content_validation.chat.TextMessageValidator;
import de.vsy.shared_transmission.packet.content.PacketContent;
import de.vsy.shared_transmission.packet.content.chat.ChatContent;

/**
 * A factory for creating chat category handler objects. Frederic Heath
 */
public class ChatPacketProcessorFactory implements ContentBasedProcessorFactory {

  private final HandlerLocalDataManager threadDataAccess;

  public ChatPacketProcessorFactory(HandlerLocalDataManager threadDataAccess) {

    this.threadDataAccess = threadDataAccess;
  }

  @Override
  public PacketProcessor createTypeProcessor(Class<? extends PacketContent> contentType) {
    var type = ChatContent.valueOf(contentType.getSimpleName());

    if (type.equals(TextMessageDTO)) {
      final var processingCondition = ContentProcessingConditionProvider.getContentProcessingCondition(
          ProcessingConditionType.ACTIVE_MESSENGER,
          this.threadDataAccess.getLocalClientStateProvider());
      return new ClientHandlerPacketProcessor<>(this.threadDataAccess.getLocalClientDataProvider(),
          processingCondition, new TextMessageValidator(),
          new TextMessageProcessor(this.threadDataAccess));
    }
    return null;
  }
}
