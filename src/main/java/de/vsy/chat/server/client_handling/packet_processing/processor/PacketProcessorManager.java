package de.vsy.chat.server.client_handling.packet_processing.processor;

import de.vsy.chat.shared_module.packet_processing.PacketProcessor;
import de.vsy.chat.shared_module.packet_processing.processor_provision.ContentBasedPacketProcessorProvider;
import de.vsy.chat.shared_module.packet_processing.processor_provision.ContentBasedProcessorFactory;
import de.vsy.chat.shared_module.packet_processing.processor_provision.PacketProcessorProvider;
import de.vsy.chat.server.client_handling.data_management.HandlerLocalDataManager;
import de.vsy.chat.server.client_handling.packet_processing.content_processor_provisioning.CategoryBasedProcessorFactoryProvider;
import de.vsy.chat.shared_transmission.packet.content.PacketContent;
import de.vsy.chat.shared_transmission.packet.property.packet_category.PacketCategory;
import de.vsy.chat.shared_transmission.packet.property.packet_identifier.ContentIdentifier;

public
class PacketProcessorManager {

    private final PacketProcessorProvider contentHandlerProvider;
    private final CategoryBasedProcessorFactoryProvider handlerProvider;
    private final HandlerLocalDataManager threadDataAccess;

    public
    PacketProcessorManager (HandlerLocalDataManager threadDataAccess,
                            CategoryBasedProcessorFactoryProvider handlerProvider) {
        this.contentHandlerProvider = new PacketProcessorProvider();
        this.threadDataAccess = threadDataAccess;
        this.handlerProvider = handlerProvider;
    }

    public
    void registerCategoryProcessingProvider (PacketCategory category,
                                             ContentBasedProcessorFactory processingProvider) {
        this.contentHandlerProvider.registerTypeProcessingProvider(category,
                                                                   new ContentBasedPacketProcessorProvider(
                                                                           processingProvider));
    }

    public
    PacketProcessor getProcessor (ContentIdentifier identifier,
                                  Class<? extends PacketContent> contentType) {
        var categoryProcessing = contentHandlerProvider.getProcessor(
                identifier.getPacketCategory(), contentType);

        if (categoryProcessing == null) {
            var factory = new ContentBasedPacketProcessorProvider(
                    this.handlerProvider.getCategoryHandlerFactory(
                            identifier.getPacketCategory(), this.threadDataAccess));
            this.contentHandlerProvider.registerTypeProcessingProvider(
                    identifier.getPacketCategory(), factory);
            categoryProcessing = factory.getProcessor(contentType);
        }
        return categoryProcessing;
    }
}
