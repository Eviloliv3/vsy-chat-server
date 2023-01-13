package de.vsy.server.client_handling.packet_processing.content_processor_provisioning;

import de.vsy.server.client_handling.data_management.HandlerLocalDataManager;
import de.vsy.shared_module.packet_processing.processor_provision.ContentBasedProcessorFactory;
import de.vsy.shared_transmission.packet.property.packet_category.PacketCategory;

public interface CategoryBasedProcessorFactoryProvider {

    /**
     * Gets a factory for the specified PacketCategory, providing access the local
     * client's data
     *
     * @param category         the factory has to provide processors for Packets of
     *                         this PacketCategory
     * @param threadDataAccess the local client's data access provider
     * @return a factory capable of providing processors for the
     * specified category, null if none are available for the PacketCategory
     */
    ContentBasedProcessorFactory getCategoryHandlerFactory(PacketCategory category,
                                                           HandlerLocalDataManager threadDataAccess);
}
