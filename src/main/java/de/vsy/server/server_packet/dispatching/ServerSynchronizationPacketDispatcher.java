/*
 *
 */
package de.vsy.server.server_packet.dispatching;

import de.vsy.server.service.Service;
import de.vsy.server.service.ServiceData;
import de.vsy.server.service.ServicePacketBufferManager;
import de.vsy.shared_module.shared_module.packet_management.PacketBuffer;
import de.vsy.shared_transmission.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.shared_transmission.packet.property.communicator.EligibleCommunicationEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Deque;
import java.util.Map;

public
class ServerSynchronizationPacketDispatcher implements MultiplePacketDispatcher {

    private static final Logger LOGGER = LogManager.getLogger();
    private final ServicePacketBufferManager serviceBuffers;
    private final Map<ServiceData.ServiceResponseDirection, Service.TYPE> responseDirections;

    public
    ServerSynchronizationPacketDispatcher (final ServicePacketBufferManager serviceBuffers,
                                           final Map<ServiceData.ServiceResponseDirection, Service.TYPE> responseDirections) {
        this.serviceBuffers = serviceBuffers;
        this.responseDirections = responseDirections;
    }

    /**
     * @param output the packet to dispatch
     *
     * @throws IllegalArgumentException wenn Paket null, Properties null, oder
     *                                  Empfänger null, EmpfängerEntität null
     */
    @Override
    public
    void dispatchPacket (final Packet output) {
        final var recipientEntity = getRecipientEntity(output);


        if (recipientEntity != null) {

            switch (recipientEntity) {
                case CLIENT -> sendInboundPacket(output);
                case SERVER -> sendOutboundPacket(output);
                default -> LOGGER.error("Ungültige Direktion.");
            }
        }
    }

    @Override
    public
    void dispatchPacket (Deque<Packet> output) {
        while(!output.isEmpty()){
            final var currentPacket = output.pop();
            this.dispatchPacket(currentPacket);
        }
    }

    private
    EligibleCommunicationEntity getRecipientEntity (Packet output) {
        EligibleCommunicationEntity recipientEntity;
        if (output != null) {
            final var recipient = output.getPacketProperties().getRecipient();

            if (recipient != null) {
                recipientEntity = recipient.getEntity();
            } else {
                throw new IllegalArgumentException("Kein Empfänger angegeben.");
            }
        } else {
            throw new IllegalArgumentException(
                    "Kein Paket zum Versenden übergeben.");
        }
        return recipientEntity;
    }

    /**
     * Regelt den Versand eines, an den Klienten gerichteten Paketes.
     *
     * @param output Das Paket vom Typ Packet dass versandt wird.
     */
    protected
    void sendInboundPacket (final Packet output) {
        if(output == null){
            throw new IllegalArgumentException("Leeres Paket wird nicht gepuffert.");
        }
        PacketBuffer buffer;

        buffer = this.serviceBuffers.getRandomBuffer(
                this.responseDirections.get(ServiceData.ServiceResponseDirection.INBOUND));

        if (buffer != null) {
            buffer.appendPacket(output);
        }
    }

    /**
     * Regelt den Versand eines, an den Server gerichteten, Paketes.
     *
     * @param output Das Paket vom Typ Packet dass versandt wird.
     */
    protected
    void sendOutboundPacket (final Packet output) {
        if(output == null){
            throw new IllegalArgumentException("Leeres Paket wird nicht gepuffert.");
        }
        PacketBuffer buffer;

        buffer = this.serviceBuffers.getRandomBuffer(
                this.responseDirections.get(ServiceData.ServiceResponseDirection.OUTBOUND));

        if (buffer != null) {
            buffer.appendPacket(output);
        }
    }
}
