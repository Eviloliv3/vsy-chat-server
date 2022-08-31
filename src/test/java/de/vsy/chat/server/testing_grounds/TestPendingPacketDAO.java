package de.vsy.chat.server.testing_grounds;

import de.vsy.shared_module.shared_module.packet_creation.ContentIdentificationProviderImpl;
import de.vsy.shared_module.shared_module.packet_creation.PacketCompiler;
import de.vsy.server.persistent_data.client_data.PendingPacketDAO;
import de.vsy.server.persistent_data.client_data.PendingType;
import de.vsy.shared_transmission.shared_transmission.dto.CommunicatorDTO;
import de.vsy.shared_transmission.shared_transmission.packet.content.relation.EligibleContactEntity;
import de.vsy.shared_transmission.shared_transmission.packet.content.status.ContactMessengerStatusDTO;
import de.vsy.shared_transmission.shared_transmission.packet.property.communicator.CommunicationEndpoint;
import org.apache.logging.log4j.LogManager;
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
        dao.setPendingPackets(PendingType.PROCESSOR_BOUND, Collections.emptyMap());
        dao.appendPendingPacket(PendingType.PROCESSOR_BOUND,
                                PacketCompiler.createRequest(
                                        CommunicationEndpoint.getClientEntity(15003),
                                        new ContactMessengerStatusDTO(
                                                EligibleContactEntity.CLIENT, true,
                                                CommunicatorDTO.valueOf(-1, ""),
                                                Collections.emptyList())));
        var pendingProcessorBound = dao.readPendingPackets(PendingType.PROCESSOR_BOUND);
        for(var pendingPacket : pendingProcessorBound.values()){
            LogManager.getLogger().debug(pendingPacket);
        }
    }

    CommunicationEndpoint getOriginator () {
        return CommunicationEndpoint.getClientEntity(15001);
    }
}
