package de.vsy.server.service.status_synchronization;

import de.vsy.server.client_management.ClientState;
import de.vsy.server.server_packet.content.ExtendedStatusSyncDTO;
import de.vsy.server.server_packet.content.builder.SimpleInternalContentBuilder;
import de.vsy.shared_module.packet_creation.PacketCompiler;
import de.vsy.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.packet.content.PacketContent;
import de.vsy.shared_transmission.packet.content.relation.EligibleContactEntity;
import de.vsy.shared_transmission.packet.content.status.ContactStatusChangeDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Set;

import static de.vsy.shared_transmission.packet.property.communicator.CommunicationEndpoint.getClientEntity;

/**
 * The Class PacketDemultiplexer.
 */
public class PacketDemultiplexer {

    private static final Logger LOGGER;

    static {
        LOGGER = LogManager.getLogger();
    }

    /**
     * Instantiates a new Packetdemultiplexer.
     */
    private PacketDemultiplexer() {
    }

    /**
     * Demultiplex Packet
     *
     * @param toDemultiplex      Packet
     * @param eligibleRecipients Set<Integer>
     * @return the list hier
     */
    public static Set<Packet> demultiplexPacket(final ExtendedStatusSyncDTO toDemultiplex,
                                                final Set<Integer> eligibleRecipients) {
        final Set<Packet> demultiplexedPackets = new HashSet<>();

        if (toDemultiplex != null && eligibleRecipients != null && !eligibleRecipients.isEmpty()) {
            final var updatePacket = createClientNotificationContent(toDemultiplex);

            if (updatePacket != null) {

                for (final int contactId : eligibleRecipients) {
                    final var currentRecipient = getClientEntity(contactId);
                    final var finalizedNotification = PacketCompiler.createRequest(currentRecipient,
                            updatePacket);
                    demultiplexedPackets.add(finalizedNotification);
                    LOGGER.info("Notification created for: {}", contactId);
                }
            } else {
                LOGGER.error("No status synchronization packets created for:\n{}", toDemultiplex);
            }
        }
        return demultiplexedPackets;
    }

    /**
     * Creates the client notification Packet
     *
     * @param clientNotification the client notification
     * @return the packet
     */
    private static PacketContent createClientNotificationContent(
            final ExtendedStatusSyncDTO clientNotification) {
        PacketContent notificationContent = null;
        final var clientStatusData = prepareNotificationContent(clientNotification);

        if (clientStatusData != null) {
            final var serverContentWrapperBuilder = new SimpleInternalContentBuilder();
            serverContentWrapperBuilder.withContent(clientStatusData)
                    .withSynchronizedServers(clientNotification.getSynchronizedServers())
                    .withOriginatingServerId(clientNotification.getOriginatingServerId());
            notificationContent = serverContentWrapperBuilder.build();
        }
        return notificationContent;
    }

    private static PacketContent prepareNotificationContent(
            final ExtendedStatusSyncDTO clientNotification) {
        final PacketContent clientStatusData;

        if (clientNotification.getClientState() == ClientState.ACTIVE_MESSENGER) {
            clientStatusData = new ContactStatusChangeDTO(EligibleContactEntity.CLIENT,
                    clientNotification.isToAdd(),
                    clientNotification.getContactData(), null);
        } else {
            clientStatusData = null;
        }
        return clientStatusData;
    }
}
