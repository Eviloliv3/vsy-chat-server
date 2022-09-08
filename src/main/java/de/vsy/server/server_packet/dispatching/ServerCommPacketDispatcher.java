/*
 *
 */
package de.vsy.server.server_packet.dispatching;

import de.vsy.server.service.Service;
import de.vsy.server.service.ServiceData.ServiceResponseDirection;
import de.vsy.server.service.ServicePacketBufferManager;
import de.vsy.shared_module.shared_module.packet_management.PacketBuffer;
import de.vsy.shared_transmission.shared_transmission.packet.Packet;

import java.util.Map;

/**
 * Special PacketDispatcher version that transmits Packet into the PacketBuffer of
 * specifiable service types. Only supports writing to one Buffer each.
 */
public
class ServerCommPacketDispatcher extends PacketDispatcherBase {

    private final Map<ServiceResponseDirection, Service.TYPE> responseDirections;
    private final ServicePacketBufferManager serviceBuffers;

    /**
     * Instantiates a new server communication Packetdispatcher.
     *
     * @param serviceBuffers the service buffers
     * @param responseDirections the response directions
     */
    public
    ServerCommPacketDispatcher (final ServicePacketBufferManager serviceBuffers,
                                final Map<ServiceResponseDirection, Service.TYPE> responseDirections) {
        this.serviceBuffers = serviceBuffers;
        this.responseDirections = responseDirections;
    }

    @Override
    protected
    void sendInboundPacket (final Packet output) {
        if(output == null){
            throw new IllegalArgumentException("Leeres Paket wird nicht gepuffert.");
        }
        PacketBuffer buffer;

        buffer = this.serviceBuffers.getRandomBuffer(
                this.responseDirections.get(ServiceResponseDirection.INBOUND));

        if (buffer != null) {
            buffer.appendPacket(output);
        }
    }

    @Override
    protected
    void sendOutboundPacket (final Packet output) {
        if(output == null){
            throw new IllegalArgumentException("Leeres Paket wird nicht gepuffert.");
        }
        PacketBuffer buffer;

        buffer = this.serviceBuffers.getRandomBuffer(
                this.responseDirections.get(ServiceResponseDirection.OUTBOUND));

        if (buffer != null) {
            buffer.appendPacket(output);
        }
    }
}
