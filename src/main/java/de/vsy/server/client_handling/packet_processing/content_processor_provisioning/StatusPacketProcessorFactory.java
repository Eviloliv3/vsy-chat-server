/*
 *
 */
package de.vsy.server.client_handling.packet_processing.content_processor_provisioning;

import de.vsy.server.client_handling.data_management.HandlerLocalDataManager;
import de.vsy.shared_module.shared_module.packet_processing.PacketProcessor;
import de.vsy.shared_module.shared_module.packet_processing.ProcessingCondition;
import de.vsy.shared_module.shared_module.packet_processing.ProcessingConditionType;
import de.vsy.shared_module.shared_module.packet_processing.processor_provision.ContentBasedProcessorFactory;
import de.vsy.shared_module.shared_module.packet_validation.content_validation.status.ClientStatusValidator;
import de.vsy.shared_module.shared_module.packet_validation.content_validation.status.ContactStatusValidator;
import de.vsy.server.client_handling.packet_processing.ClientHandlerPacketProcessor;
import de.vsy.server.client_handling.packet_processing.content_processing.ClientStatusChangeProcessor;
import de.vsy.server.client_handling.packet_processing.content_processing.ContactStatusChangeProcessor;
import de.vsy.shared_transmission.shared_transmission.packet.content.PacketContent;
import de.vsy.shared_transmission.shared_transmission.packet.content.status.StatusContent;

/** A factory for creating udpate category handlers. Frederic Heath */
public
class StatusPacketProcessorFactory implements ContentBasedProcessorFactory {

    private final HandlerLocalDataManager threadDataAccess;

    public
    StatusPacketProcessorFactory (HandlerLocalDataManager threadDataAccess) {
        this.threadDataAccess = threadDataAccess;
    }

    @Override
    public
    PacketProcessor createTypeProcessor (
            final Class<? extends PacketContent> contentType) {
        ProcessingCondition processingCondition;
        final var type = StatusContent.valueOf(contentType.getSimpleName());

        switch (type) {
            case ClientStatusChangeDTO -> {
                processingCondition = ContentProcessingConditionProvider.getContentProcessingCondition(
                        ProcessingConditionType.AUTHENTICATED,
                        this.threadDataAccess.getLocalClientStateProvider());
                return new ClientHandlerPacketProcessor<>(
                        this.threadDataAccess.getLocalClientDataProvider(),
                        processingCondition, new ClientStatusValidator(),
                        new ClientStatusChangeProcessor(this.threadDataAccess));
            }
            case ContactMessengerStatusDTO -> {
                processingCondition = ContentProcessingConditionProvider.getContentProcessingCondition(
                        ProcessingConditionType.ACTIVE_MESSENGER,
                        this.threadDataAccess.getLocalClientStateProvider());
                return new ClientHandlerPacketProcessor<>(
                        this.threadDataAccess.getLocalClientDataProvider(),
                        processingCondition, new ContactStatusValidator(),
                        new ContactStatusChangeProcessor(this.threadDataAccess));
            }
            default -> {
            }
        }
        return null;
    }
}
