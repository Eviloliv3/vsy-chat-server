package de.vsy.server.client_handling.packet_processing.content_processor_provisioning;

import de.vsy.server.client_handling.data_management.HandlerLocalDataManager;
import de.vsy.server.client_handling.packet_processing.ClientHandlerPacketProcessor;
import de.vsy.server.client_handling.packet_processing.content_processing.RelationRequestProcessor;
import de.vsy.server.client_handling.packet_processing.content_processing.RelationResponseProcessor;
import de.vsy.shared_module.packet_processing.PacketProcessor;
import de.vsy.shared_module.packet_processing.ProcessingCondition;
import de.vsy.shared_module.packet_processing.ProcessingConditionType;
import de.vsy.shared_module.packet_processing.processor_provision.ContentBasedProcessorFactory;
import de.vsy.shared_module.packet_validation.content_validation.relation.ContactRelationRequestValidator;
import de.vsy.shared_module.packet_validation.content_validation.relation.ContactRelationResponseValidator;
import de.vsy.shared_transmission.packet.content.PacketContent;
import de.vsy.shared_transmission.packet.content.relation.RelationContent;

public class RelationPacketProcessorFactory implements ContentBasedProcessorFactory {

    private final HandlerLocalDataManager threadDataAccess;

    public RelationPacketProcessorFactory(HandlerLocalDataManager threadDataAccess) {
        this.threadDataAccess = threadDataAccess;
    }

    @Override
    public PacketProcessor createTypeProcessor(final Class<? extends PacketContent> contentType) {
        ProcessingCondition processingCondition;
        final var type = RelationContent.valueOf(contentType.getSimpleName());

        switch (type) {
            case ContactRelationRequestDTO -> {
                processingCondition = ContentProcessingConditionProvider.getContentProcessingCondition(
                        ProcessingConditionType.AUTHENTICATED,
                        this.threadDataAccess.getLocalClientStateProvider());
                return new ClientHandlerPacketProcessor<>(
                        this.threadDataAccess.getLocalClientDataProvider(),
                        processingCondition, new ContactRelationRequestValidator(),
                        new RelationRequestProcessor(this.threadDataAccess));
            }
            case ContactRelationResponseDTO -> {
                processingCondition = ContentProcessingConditionProvider.getContentProcessingCondition(
                        ProcessingConditionType.AUTHENTICATED,
                        this.threadDataAccess.getLocalClientStateProvider());
                return new ClientHandlerPacketProcessor<>(
                        this.threadDataAccess.getLocalClientDataProvider(),
                        processingCondition, new ContactRelationResponseValidator(),
                        new RelationResponseProcessor(this.threadDataAccess));
            }
            default -> {
            }
        }
        return null;
    }
}
