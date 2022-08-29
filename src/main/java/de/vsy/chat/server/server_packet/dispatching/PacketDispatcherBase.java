/*
 *
 */
package de.vsy.chat.server.server_packet.dispatching;

import de.vsy.chat.shared_transmission.packet.Packet;
import de.vsy.chat.shared_transmission.packet.property.communicator.EligibleCommunicationEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract
class PacketDispatcherBase implements PacketDispatcher {

    private static final Logger LOGGER = LogManager.getLogger();

    protected
    PacketDispatcherBase () {
        super();
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

    private
    EligibleCommunicationEntity getRecipientEntity (Packet output) {
        EligibleCommunicationEntity recipientEntity;
        if (output != null) {
            final var recipient = output.getPacketProperties().getRecipientEntity();

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
    protected abstract
    void sendInboundPacket (Packet output);

    /**
     * Regelt den Versand eines, an den Server gerichteten, Paketes.
     *
     * @param output Das Paket vom Typ Packet dass versandt wird.
     */
    protected abstract
    void sendOutboundPacket (Packet output);
}
