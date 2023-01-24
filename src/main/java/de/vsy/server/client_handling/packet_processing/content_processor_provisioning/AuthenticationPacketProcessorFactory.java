package de.vsy.server.client_handling.packet_processing.content_processor_provisioning;

import de.vsy.server.client_handling.data_management.HandlerLocalDataManager;
import de.vsy.server.client_handling.packet_processing.ClientHandlerPacketProcessor;
import de.vsy.server.client_handling.packet_processing.content_processing.*;
import de.vsy.shared_module.packet_processing.PacketProcessor;
import de.vsy.shared_module.packet_processing.ProcessingCondition;
import de.vsy.shared_module.packet_processing.processor_provision.ContentBasedProcessorFactory;
import de.vsy.shared_module.packet_validation.content_validation.authentication.*;
import de.vsy.shared_transmission.packet.content.PacketContent;

import static de.vsy.shared_module.packet_processing.ProcessingConditionType.AUTHENTICATED;
import static de.vsy.shared_module.packet_processing.ProcessingConditionType.NOT_AUTHENTICATED;
import static de.vsy.shared_transmission.packet.content.authentication.AuthenticationContent.valueOf;

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
            case AccountCreationRequestDTO -> {
                processingCondition = ContentProcessingConditionProvider.getContentProcessingCondition(
                        NOT_AUTHENTICATED, this.threadDataAccess.getLocalClientStateProvider());
                return new ClientHandlerPacketProcessor<>(
                        this.threadDataAccess.getLocalClientDataProvider(),
                        processingCondition, new NewAccountRequestValidator(),
                        new AccountCreationProcessor(this.threadDataAccess));
            }
            case AccountDeletionRequestDTO -> {
                processingCondition = ContentProcessingConditionProvider.getContentProcessingCondition(
                        AUTHENTICATED, this.threadDataAccess.getLocalClientStateProvider());
                return new ClientHandlerPacketProcessor<>(
                        this.threadDataAccess.getLocalClientDataProvider(),
                        processingCondition, new AccountDeletionRequestValidator(),
                        new AccountDeletionProcessor(this.threadDataAccess));
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
