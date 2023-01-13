package de.vsy.chat.server.testing_grounds;

import de.vsy.server.persistent_data.client_data.PendingPacketDAO;
import de.vsy.server.persistent_data.client_data.PendingType;
import de.vsy.shared_module.packet_creation.ContentIdentificationProviderImpl;
import de.vsy.shared_module.packet_creation.PacketCompiler;
import de.vsy.shared_transmission.dto.CommunicatorDTO;
import de.vsy.shared_transmission.packet.content.relation.ContactRelationRequestDTO;
import de.vsy.shared_transmission.packet.content.relation.EligibleContactEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static de.vsy.shared_transmission.packet.property.communicator.CommunicationEndpoint.getClientEntity;
import static de.vsy.shared_transmission.packet.property.communicator.CommunicationEndpoint.getServerEntity;
import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_SERVER_ID;

class TestPacketSerialization {

    private PendingPacketDAO pendingPacketAccessor;
    private PacketSaver saver;

    @BeforeEach
    public void createFileAccess() throws InterruptedException {
        this.pendingPacketAccessor = new PendingPacketDAO();
        pendingPacketAccessor.createAccess(String.valueOf(16000));
        this.saver = new PacketSaver();
        saver.createAccess(String.valueOf(16000));
    }

    @AfterEach
    public void removeFileAccess() {
        pendingPacketAccessor.removeFileAccess();
        saver.removeFileAccess();
    }

    @Test
    public void saveRelationRequest() {
        PacketCompiler.addOriginatorEntityProvider(() -> getClientEntity(16000));
        PacketCompiler.addContentIdentificationProvider(new ContentIdentificationProviderImpl());
        var contactRelationChangeDTO = new ContactRelationRequestDTO(EligibleContactEntity.CLIENT,
                15001, 15000,
                CommunicatorDTO.valueOf(15001, "Test Name"), true);
        var request = PacketCompiler.createRequest(getServerEntity(STANDARD_SERVER_ID),
                contactRelationChangeDTO);
        saver.savePacket(request);
        Assertions.assertTrue(
                pendingPacketAccessor.appendPendingPacket(PendingType.PROCESSOR_BOUND, request));
    }

    @Test
    public void readPacket() {
        // var readPacket = saver.readPacket();
        var nullPacket = false;
        var pendingPacketList = this.pendingPacketAccessor.readPendingPackets(
                PendingType.PROCESSOR_BOUND);
        for (var packet : pendingPacketList.entrySet()) {
            nullPacket = packet == null;
        }
        Assertions.assertFalse(nullPacket);
    }
}
