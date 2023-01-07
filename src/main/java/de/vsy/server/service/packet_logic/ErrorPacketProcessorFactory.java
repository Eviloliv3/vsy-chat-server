package de.vsy.server.service.packet_logic;

import de.vsy.server.data.access.ErrorHandlingServiceDataProvider;
import de.vsy.server.server_packet.packet_properties.packet_type.ServerErrorType;
import de.vsy.server.service.packet_logic.type_processor.ServerErrorPacketProcessor;
import de.vsy.shared_transmission.packet.property.packet_type.PacketType;

import java.util.HashMap;
import java.util.Map;

/**
 * A factory for creating ErrorPacketProcessor objects.
 */
public class ErrorPacketProcessorFactory implements ServicePacketProcessorFactory {

    private final ErrorHandlingServiceDataProvider serviceDataModel;
    private Map<PacketType, ServicePacketProcessor> registeredTypeHandlers;

    /**
     * Instantiates a new error PacketHandler factory.
     *
     * @param serviceDataModel the service dataManagement model
     */
    public ErrorPacketProcessorFactory(final ErrorHandlingServiceDataProvider serviceDataModel) {
        this.serviceDataModel = serviceDataModel;
        registerHandlers();
    }

    private void registerHandlers() {
        this.registeredTypeHandlers = new HashMap<>();
        this.registeredTypeHandlers.put(ServerErrorType.SERVER_STATUS,
                new ServerErrorPacketProcessor(this.serviceDataModel));
    }

    @Override
    public ServicePacketProcessor getPacketProcessor(final PacketType type) {
        return this.registeredTypeHandlers.get(type);
    }
}
