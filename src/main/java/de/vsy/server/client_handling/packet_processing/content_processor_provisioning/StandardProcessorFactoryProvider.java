package de.vsy.server.client_handling.packet_processing.content_processor_provisioning;

import de.vsy.server.client_handling.data_management.HandlerLocalDataManager;
import de.vsy.shared_module.packet_processing.processor_provision.ContentBasedProcessorFactory;
import de.vsy.shared_transmission.packet.property.packet_category.PacketCategory;

public class StandardProcessorFactoryProvider implements CategoryBasedProcessorFactoryProvider {

    @Override
    public ContentBasedProcessorFactory getCategoryHandlerFactory(PacketCategory category,
                                                                  HandlerLocalDataManager threadDataAccess) {
        if (category == null) {
            return null;
        }

        return switch (category) {
            case AUTHENTICATION ->
                    new AuthenticationPacketProcessorFactory(threadDataAccess);
            case CHAT -> new ChatPacketProcessorFactory(threadDataAccess);
            case STATUS -> new StatusPacketProcessorFactory(threadDataAccess);
            case RELATION -> new RelationPacketProcessorFactory(threadDataAccess);
            case NOTIFICATION ->
                    new NotificationPacketProcessorFactory(threadDataAccess);
        };
    }
}
