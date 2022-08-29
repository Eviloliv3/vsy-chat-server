package de.vsy.chat.server.testing_grounds;

import de.vsy.chat.shared_module.packet_creation.ContentIdentificationProviderImpl;
import de.vsy.chat.shared_module.packet_creation.PacketCompiler;
import de.vsy.chat.server.persistent_data.client_data.PendingPacketDAO;
import de.vsy.chat.server.persistent_data.client_data.PendingType;
import de.vsy.chat.shared_transmission.dto.CommunicatorDTO;
import de.vsy.chat.shared_transmission.packet.content.relation.EligibleContactEntity;
import de.vsy.chat.shared_transmission.packet.content.status.ContactMessengerStatusDTO;
import de.vsy.chat.shared_transmission.packet.property.communicator.CommunicationEndpoint;
import org.junit.jupiter.api.Test;

import java.util.Collections;

public
class TestPendingPacketDAO {

    @Test
    void writePackets ()
    throws InterruptedException {
        PacketCompiler.addOriginatorEntityProvider(this::getOriginator);
        PacketCompiler.addContentIdentificator(
                new ContentIdentificationProviderImpl());
        var dao = new PendingPacketDAO();
        dao.createFileAccess(15001);
        dao.appendPendingPacket(PendingType.PROCESSOR_BOUND,
                                PacketCompiler.createRequest(
                                        CommunicationEndpoint.getClientEntity(15003),
                                        new ContactMessengerStatusDTO(
                                                EligibleContactEntity.CLIENT, true,
                                                CommunicatorDTO.valueOf(-1, ""),
                                                Collections.emptyList())));
    }

    CommunicationEndpoint getOriginator () {
        return CommunicationEndpoint.getClientEntity(15001);
    }
}
