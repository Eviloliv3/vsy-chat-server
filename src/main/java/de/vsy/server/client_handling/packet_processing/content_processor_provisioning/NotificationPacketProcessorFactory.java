package de.vsy.server.client_handling.packet_processing.content_processor_provisioning;

import de.vsy.server.client_handling.data_management.HandlerLocalDataManager;
import de.vsy.server.client_handling.packet_processing.ClientHandlerPacketProcessor;
import de.vsy.server.client_handling.packet_processing.content_processing.NotificationProcessor;
import de.vsy.shared_module.packet_processing.PacketProcessor;
import de.vsy.shared_module.packet_processing.ProcessingCondition;
import de.vsy.shared_module.packet_processing.processor_provision.ContentBasedProcessorFactory;
import de.vsy.shared_module.packet_validation.content_validation.error.NotificationContentValidator;
import de.vsy.shared_transmission.packet.content.PacketContent;

import static de.vsy.shared_transmission.packet.content.notification.NotificationContent.ErrorDTO;
import static de.vsy.shared_transmission.packet.content.notification.NotificationContent.SimpleInformationDTO;
import static de.vsy.shared_transmission.packet.content.notification.NotificationContent.valueOf;

public class NotificationPacketProcessorFactory implements ContentBasedProcessorFactory {

    private final HandlerLocalDataManager threadDataAccess;

    public NotificationPacketProcessorFactory(HandlerLocalDataManager threadDataAccess) {

        this.threadDataAccess = threadDataAccess;
    }

    @Override
    public PacketProcessor createTypeProcessor(final Class<? extends PacketContent> contentType) {
        ProcessingCondition processingCondition;
        final var type = valueOf(contentType.getSimpleName());

        if (type.equals(ErrorDTO) || type.equals(SimpleInformationDTO)) {
            processingCondition = getSimpleErrorProcessingCondition();
            return new ClientHandlerPacketProcessor<>(this.threadDataAccess.getLocalClientDataProvider(),
                    processingCondition, new NotificationContentValidator(),
                    new NotificationProcessor(this.threadDataAccess));
        } else {
            return null;
        }
    }

    private ProcessingCondition getSimpleErrorProcessingCondition() {
        return new ProcessingCondition() {
            @Override
            public boolean checkCondition() {
                return true;
            }

            @Override
            public String getErrorMessage() {
                return "This error message should never be displayed, because the checkCondition() method always returns true.";
            }
        };
    }
}
