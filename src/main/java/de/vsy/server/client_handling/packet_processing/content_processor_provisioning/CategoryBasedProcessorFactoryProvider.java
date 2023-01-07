package de.vsy.server.client_handling.packet_processing.content_processor_provisioning;

import de.vsy.server.client_handling.data_management.HandlerLocalDataManager;
import de.vsy.shared_module.packet_processing.processor_provision.ContentBasedProcessorFactory;
import de.vsy.shared_transmission.packet.property.packet_category.PacketCategory;

public interface CategoryBasedProcessorFactoryProvider {

    ContentBasedProcessorFactory getCategoryHandlerFactory(PacketCategory category,
                                                           HandlerLocalDataManager threadDataAccess);
}
