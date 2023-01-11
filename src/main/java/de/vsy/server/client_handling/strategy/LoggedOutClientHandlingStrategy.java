package de.vsy.server.client_handling.strategy;

import de.vsy.server.persistent_data.client_data.PendingPacketDAO;
import de.vsy.server.persistent_data.client_data.PendingType;
import de.vsy.shared_module.packet_creation.PacketCompiler;
import de.vsy.shared_module.packet_management.ThreadPacketBufferManager;
import de.vsy.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.packet.content.PacketContent;
import de.vsy.shared_transmission.packet.content.chat.TextMessageDTO;
import de.vsy.shared_transmission.packet.content.error.ErrorDTO;
import de.vsy.shared_transmission.packet.content.relation.ContactRelationResponseDTO;

import java.util.Set;

import static de.vsy.shared_module.packet_management.ThreadPacketBufferLabel.HANDLER_BOUND;
import static de.vsy.shared_module.packet_management.ThreadPacketBufferLabel.SERVER_BOUND;

public class LoggedOutClientHandlingStrategy implements PacketHandlingStrategy {

    private static final Set<Class<? extends PacketContent>> retainableContentTypes;

    static {
        retainableContentTypes = Set.of(TextMessageDTO.class, ContactRelationResponseDTO.class,
                ErrorDTO.class);
    }

    private final ThreadPacketBufferManager bufferManager;

    public LoggedOutClientHandlingStrategy(ThreadPacketBufferManager bufferManager) {
        this.bufferManager = bufferManager;
    }

    @Override
    public void administerStrategy() {
        var handlerBoundBuffer = this.bufferManager.getPacketBuffer(HANDLER_BOUND);
        var remainingPackets = handlerBoundBuffer.freezeBuffer();

        for (var currentPacket : remainingPackets) {
            var packetContent = currentPacket.getPacketContent();

            if (retainableContentTypes.contains(packetContent.getClass())) {
                retainPacket(currentPacket);
            } else {
                sendOfflineResponse(currentPacket);
            }
        }
    }

    private void sendOfflineResponse(final Packet origin) {
        var contactOffline = new ErrorDTO("Packet could not be delivered. Contact is offline.", origin);
        var errorPacket = PacketCompiler.createResponse(contactOffline, origin);
        this.bufferManager.getPacketBuffer(SERVER_BOUND).appendPacket(errorPacket);
    }

    private void retainPacket(final Packet retainablePacket) {
        PendingPacketDAO p = new PendingPacketDAO();
        p.createFileAccess(retainablePacket.getPacketProperties().getRecipient().getEntityId());
        p.appendPendingPacket(PendingType.PROCESSOR_BOUND, retainablePacket);
    }
}
