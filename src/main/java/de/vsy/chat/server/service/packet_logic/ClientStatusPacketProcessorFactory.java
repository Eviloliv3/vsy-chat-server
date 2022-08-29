/*
 *
 */
package de.vsy.chat.server.service.packet_logic;

import de.vsy.chat.server.server.data.access.ClientStatusRegistrationServiceDataProvider;
import de.vsy.chat.server.server_packet.packet_properties.packet_type.ServerStatusType;
import de.vsy.chat.server.service.packet_logic.type_processor.ClientStatusSyncPacketProcessor;
import de.vsy.chat.shared_transmission.packet.property.packet_type.PacketType;

import java.util.HashMap;
import java.util.Map;

/**
 * A factory for creating ServicePacketProcessors for status category Packet.
 */
public
class ClientStatusPacketProcessorFactory implements ServicePacketProcessorFactory {

    /** The registered type handlers. */
    private final Map<PacketType, ServicePacketProcessor> registeredTypeHandlers;
    /** The server dataManagement model. */
    private final ClientStatusRegistrationServiceDataProvider serviceDataModel;

    /**
     * Instantiates a new client status sync PacketHandler factory.
     *
     * @param serviceDataAccess the service dataManagement accessLimiter
     */
    public
    ClientStatusPacketProcessorFactory (
            final ClientStatusRegistrationServiceDataProvider serviceDataAccess) {
        this.registeredTypeHandlers = new HashMap<>();
        this.serviceDataModel = serviceDataAccess;

        registerHandlers();
    }

    /** Register handlers. */
    private
    void registerHandlers () {
        this.registeredTypeHandlers.put(ServerStatusType.CLIENT_STATUS,
                                        new ClientStatusSyncPacketProcessor(
                                                this.serviceDataModel));
    }

    /**
     * Gets the PacketHandler.
     *
     * @param type the type
     *
     * @return the PacketHandler
     */
    @Override
    public
    ServicePacketProcessor getPacketProcessor (final PacketType type) {
        return this.registeredTypeHandlers.get(type);
    }
}
