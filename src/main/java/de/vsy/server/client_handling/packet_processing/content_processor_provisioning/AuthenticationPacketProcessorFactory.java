/*
 *
 */
package de.vsy.server.client_handling.packet_processing.content_processor_provisioning;

import static de.vsy.shared_module.packet_processing.ProcessingConditionType.AUTHENTICATED;
import static de.vsy.shared_module.packet_processing.ProcessingConditionType.NOT_AUTHENTICATED;
import static de.vsy.shared_transmission.packet.content.authentication.AuthenticationContent.valueOf;

import de.vsy.server.client_handling.data_management.HandlerLocalDataManager;
import de.vsy.server.client_handling.packet_processing.ClientHandlerPacketProcessor;
import de.vsy.server.client_handling.packet_processing.content_processing.AccountCreationProcessor;
import de.vsy.server.client_handling.packet_processing.content_processing.LoginRequestProcessor;
import de.vsy.server.client_handling.packet_processing.content_processing.LogoutRequestProcessor;
import de.vsy.server.client_handling.packet_processing.content_processing.ReconnectRequestProcessor;
import de.vsy.shared_module.packet_processing.PacketProcessor;
import de.vsy.shared_module.packet_processing.ProcessingCondition;
import de.vsy.shared_module.packet_processing.processor_provision.ContentBasedProcessorFactory;
import de.vsy.shared_module.packet_validation.content_validation.authentication.LoginRequestValidator;
import de.vsy.shared_module.packet_validation.content_validation.authentication.LogoutRequestValidator;
import de.vsy.shared_module.packet_validation.content_validation.authentication.NewAccountRequestValidator;
import de.vsy.shared_module.packet_validation.content_validation.authentication.ReconnectRequestValidator;
import de.vsy.shared_transmission.packet.content.PacketContent;

public class AuthenticationPacketProcessorFactory implements ContentBasedProcessorFactory {

  private final HandlerLocalDataManager threadDataAccess;

  public AuthenticationPacketProcessorFactory(final HandlerLocalDataManager threadDataAccess) {
    this.threadDataAccess = threadDataAccess;
  }

  @Override
  public PacketProcessor createTypeProcessor(Class<? extends PacketContent> contentType) {
    ProcessingCondition processingCondition;
    var type = valueOf(contentType.getSimpleName());

    switch (type) {
      case LoginRequestDTO -> {
        processingCondition = ContentProcessingConditionProvider.getContentProcessingCondition(
            NOT_AUTHENTICATED, this.threadDataAccess.getLocalClientStateProvider());
        return new ClientHandlerPacketProcessor<>(
            this.threadDataAccess.getLocalClientDataProvider(),
            processingCondition, new LoginRequestValidator(),
            new LoginRequestProcessor(this.threadDataAccess));
      }
      case LogoutRequestDTO -> {
        processingCondition = ContentProcessingConditionProvider.getContentProcessingCondition(
            AUTHENTICATED, this.threadDataAccess.getLocalClientStateProvider());
        return new ClientHandlerPacketProcessor<>(
            this.threadDataAccess.getLocalClientDataProvider(),
            processingCondition, new LogoutRequestValidator(),
            new LogoutRequestProcessor(this.threadDataAccess));
      }
      case NewAccountRequestDTO -> {
        processingCondition = ContentProcessingConditionProvider.getContentProcessingCondition(
            NOT_AUTHENTICATED, this.threadDataAccess.getLocalClientStateProvider());
        return new ClientHandlerPacketProcessor<>(
            this.threadDataAccess.getLocalClientDataProvider(),
            processingCondition, new NewAccountRequestValidator(),
            new AccountCreationProcessor(this.threadDataAccess));
      }
      case ReconnectRequestDTO -> {
        processingCondition = ContentProcessingConditionProvider.getContentProcessingCondition(
            NOT_AUTHENTICATED, this.threadDataAccess.getLocalClientStateProvider());
        return new ClientHandlerPacketProcessor<>(
            this.threadDataAccess.getLocalClientDataProvider(),
            processingCondition, new ReconnectRequestValidator(),
            new ReconnectRequestProcessor(this.threadDataAccess));
      }
      default -> {
      }
    }
    return null;
  }
}
