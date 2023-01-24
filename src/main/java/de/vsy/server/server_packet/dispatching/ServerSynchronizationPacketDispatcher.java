package de.vsy.server.server_packet.dispatching;

import de.vsy.server.service.Service;
import de.vsy.server.service.ServiceData;
import de.vsy.server.service.ServicePacketBufferManager;
import de.vsy.shared_module.packet_management.MultiplePacketDispatcher;
import de.vsy.shared_module.packet_management.PacketBuffer;
import de.vsy.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.packet.property.communicator.EligibleCommunicationEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Deque;
import java.util.Map;

public class ServerSynchronizationPacketDispatcher implements MultiplePacketDispatcher {

    private static final Logger LOGGER = LogManager.getLogger();
    private final ServicePacketBufferManager serviceBuffers;
    private final Map<ServiceData.ServiceResponseDirection, Service.TYPE> responseDirections;

    public ServerSynchronizationPacketDispatcher(final ServicePacketBufferManager serviceBuffers,
                                                 final Map<ServiceData.ServiceResponseDirection, Service.TYPE> responseDirections) {
        this.serviceBuffers = serviceBuffers;
        this.responseDirections = responseDirections;
    }

    @Override
    public void dispatchPacket(Deque<Packet> output) {
        while (!output.isEmpty()) {
            final var currentPacket = output.pop();
            this.dispatchPacket(currentPacket);
        }
    }


    @Override
    public void dispatchPacket(final Packet output) {
        final var recipientEntity = getRecipientEntity(output);

        if (recipientEntity != null) {

            switch (recipientEntity) {
                case CLIENT -> sendInboundPacket(output);
                case SERVER -> sendOutboundPacket(output);
                default -> LOGGER.error("Invalid direction.");
            }
        }
    }

    private EligibleCommunicationEntity getRecipientEntity(Packet output) {
        EligibleCommunicationEntity recipientEntity;
        if (output != null) {
            final var recipient = output.getPacketProperties().getRecipient();

            if (recipient != null) {
                recipientEntity = recipient.getEntity();
            } else {
                throw new IllegalArgumentException("No recipient specified.");
            }
        } else {
            throw new IllegalArgumentException("No Packet specified.");
        }
        return recipientEntity;
    }

    /**
     * Dispatches client bound Packets.
     *
     * @param output Packet
     */
    protected void sendInboundPacket(final Packet output) {
        if (output == null) {
            throw new IllegalArgumentException("No Packet specified.");
        }
        PacketBuffer buffer;

        buffer = this.serviceBuffers
                .getRandomBuffer(this.responseDirections.get(ServiceData.ServiceResponseDirection.INBOUND));

        if (buffer != null) {
            buffer.appendPacket(output);
        }
    }

    /**
     * Dispatches server bound Packets.
     *
     * @param output Packet
     */
    protected void sendOutboundPacket(final Packet output) {
        if (output == null) {
            throw new IllegalArgumentException("No Packet specified.");
        }
        PacketBuffer buffer;

        buffer = this.serviceBuffers
                .getRandomBuffer(
                        this.responseDirections.get(ServiceData.ServiceResponseDirection.OUTBOUND));

        if (buffer != null) {
            buffer.appendPacket(output);
        }
    }
}
