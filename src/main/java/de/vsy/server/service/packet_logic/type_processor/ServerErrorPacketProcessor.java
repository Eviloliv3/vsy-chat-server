package de.vsy.server.service.packet_logic.type_processor;

import de.vsy.server.data.access.ErrorHandlingServiceDataProvider;
import de.vsy.server.server_packet.content.ServerFailureDTO;
import de.vsy.shared_module.packet_processing.PacketProcessor;
import de.vsy.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.packet.content.PacketContent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The Class ServerErrorPacketProcessor.
 */
public class ServerErrorPacketProcessor implements PacketProcessor {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Instantiates a new server error PacketHandler.
     *
     * @param serviceDataProvider the dataManagement accessLimiter provider
     */
    public ServerErrorPacketProcessor(final ErrorHandlingServiceDataProvider serviceDataProvider) {

    }

    @Override
    public void processPacket(final Packet input) {
        PacketContent content;

        content = input.getPacketContent();

        if (!(content instanceof ServerFailureDTO)) {
            LOGGER.info("Invalid data format.");
        }
    }
}
