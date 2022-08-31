package de.vsy.server.client_handling.packet_processing.content_processor_provisioning;

import de.vsy.server.client_handling.data_management.HandlerLocalDataManager;
import de.vsy.server.client_handling.packet_processing.ClientHandlerPacketProcessor;
import de.vsy.server.client_handling.packet_processing.content_processing.ErrorTransmissionProcessor;
import de.vsy.shared_module.shared_module.packet_processing.PacketProcessor;
import de.vsy.shared_module.shared_module.packet_processing.ProcessingCondition;
import de.vsy.shared_module.shared_module.packet_processing.processor_provision.ContentBasedProcessorFactory;
import de.vsy.shared_module.shared_module.packet_validation.content_validation.error.ErrorContentValidator;
import de.vsy.shared_transmission.shared_transmission.packet.content.PacketContent;

import static de.vsy.shared_transmission.shared_transmission.packet.content.error.ErrorContent.ErrorDTO;
import static de.vsy.shared_transmission.shared_transmission.packet.content.error.ErrorContent.valueOf;

public
class ErrorPacketProcessorFactory implements ContentBasedProcessorFactory {

    private final HandlerLocalDataManager threadDataAccess;

    public
    ErrorPacketProcessorFactory (HandlerLocalDataManager threadDataAccess) {

        this.threadDataAccess = threadDataAccess;
    }

    @Override
    public
    PacketProcessor createTypeProcessor (
            final Class<? extends PacketContent> contentType) {
        ProcessingCondition processingCondition;
        final var type = valueOf(contentType.getSimpleName());

        if (type.equals(ErrorDTO)) {
            processingCondition = getSimpleErrorProcessingCondition();
            return new ClientHandlerPacketProcessor<>(
                    this.threadDataAccess.getLocalClientDataProvider(),
                    processingCondition, new ErrorContentValidator(),
                    new ErrorTransmissionProcessor(this.threadDataAccess));
        } else {
            return null;
        }
    }

    private
    ProcessingCondition getSimpleErrorProcessingCondition () {
        return new ProcessingCondition() {
            @Override
            public
            boolean checkCondition () {
                return true;
            }

            @Override
            public
            String getErrorMessage () {
                return "Diese Fehlernachricht sollte nie abgefragt werden.";
            }
        };
    }
}
