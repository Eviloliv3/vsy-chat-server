package de.vsy.server.service.packet_logic.type_processor;

import de.vsy.server.server.data.access.ErrorHandlingServiceDataProvider;
import de.vsy.server.server_packet.content.ServerFailureDTO;
import de.vsy.server.service.packet_logic.PacketResponseMap;
import de.vsy.server.service.packet_logic.ServicePacketProcessor;
import de.vsy.shared_transmission.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.shared_transmission.packet.content.PacketContent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** The Class ServerErrorPacketProcessor. */
public
class ServerErrorPacketProcessor implements ServicePacketProcessor {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * threadsicheres Flag; zeigt an, ob bestehende Klientenverbindungen nach
     * Serverstart geladen wurden.
     */
    /**
     * Instantiates a new server error PacketHandler.
     *
     * @param serviceDataProvider the dataManagement accessLimiter provider
     */
    public
    ServerErrorPacketProcessor (
            final ErrorHandlingServiceDataProvider serviceDataProvider) {

    }

    @Override
    public
    PacketResponseMap processPacket (final Packet input) {
        PacketContent content;

        content = input.getPacketContent();

        if (!(content instanceof ServerFailureDTO)) {
            LOGGER.info("Ungültiges Datenformat.");
        }

        return null;
    }
}