/*
 *
 */
package de.vsy.server.service.packet_logic;

import de.vsy.server.data.access.ClientStatusRegistrationServiceDataProvider;
import de.vsy.server.server_packet.packet_creation.ResultingPacketContentHandler;
import de.vsy.server.server_packet.packet_properties.packet_type.ServerStatusType;
import de.vsy.server.service.packet_logic.type_processor.ClientStatusSyncPacketProcessor;
import de.vsy.shared_transmission.packet.property.packet_type.PacketType;

import java.util.HashMap;
import java.util.Map;

/**
 * A factory for creating ServicePacketProcessors for status category Packet.
 */
public class ClientStatusPacketProcessorFactory implements ServicePacketProcessorFactory {

    /**
     * The registered type handlers.
     */
    private final Map<PacketType, ServicePacketProcessor> registeredTypeHandlers;
    /**
     * The server dataManagement model.
     */
    private final ClientStatusRegistrationServiceDataProvider serviceDataModel;

    /**
     * Instantiates a new client status sync PacketHandler factory.
     *
     * @param serviceDataAccess the service dataManagement accessLimiter
     */
    public ClientStatusPacketProcessorFactory(final ResultingPacketContentHandler resultingPackets,
                                              final ClientStatusRegistrationServiceDataProvider serviceDataAccess) {
        this.registeredTypeHandlers = new HashMap<>();
        this.serviceDataModel = serviceDataAccess;

        registerHandlers(resultingPackets);
    }

    /**
     * Register handlers.
     */
    private void registerHandlers(final ResultingPacketContentHandler resultingPackets) {
        this.registeredTypeHandlers.put(ServerStatusType.CLIENT_STATUS,
                new ClientStatusSyncPacketProcessor(resultingPackets, this.serviceDataModel));
    }

    /**
     * Returns the PacketHandler.
     *
     * @param type the type
     * @return the PacketHandler
     */
    @Override
    public ServicePacketProcessor getPacketProcessor(final PacketType type) {
        return this.registeredTypeHandlers.get(type);
    }
}
