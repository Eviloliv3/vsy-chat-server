package de.vsy.chat.server.testing_grounds;

import de.vsy.server.persistent_data.data_bean.CommunicatorData;
import de.vsy.server.persistent_data.data_bean.ConvertCommDataToDTO;
import de.vsy.server.server.client_management.ClientState;
import de.vsy.server.server_packet.content.*;
import de.vsy.server.server_packet.content.builder.ExtendedStatusSyncBuilder;
import de.vsy.server.server_packet.content.builder.ServerFailureContentBuilder;
import de.vsy.server.server_packet.content.builder.SimpleStatusSyncBuilder;
import de.vsy.shared_module.shared_module.packet_creation.ContentIdentificationProviderImpl;
import de.vsy.shared_module.shared_module.packet_creation.PacketCompiler;
import de.vsy.server.persistent_data.client_data.PendingPacketDAO;
import de.vsy.server.persistent_data.client_data.PendingType;
import de.vsy.shared_transmission.shared_transmission.dto.authentication.AuthenticationDTO;
import de.vsy.shared_transmission.shared_transmission.packet.content.PacketContent;
import de.vsy.shared_transmission.shared_transmission.packet.content.authentication.*;
import de.vsy.shared_transmission.shared_transmission.packet.content.chat.TextMessageDTO;
import de.vsy.shared_transmission.shared_transmission.packet.content.error.ErrorDTO;
import de.vsy.shared_transmission.shared_transmission.packet.content.relation.ContactRelationRequestDTO;
import de.vsy.shared_transmission.shared_transmission.packet.content.relation.ContactRelationResponseDTO;
import de.vsy.shared_transmission.shared_transmission.packet.content.relation.EligibleContactEntity;
import de.vsy.shared_transmission.shared_transmission.packet.content.status.*;
import de.vsy.shared_transmission.shared_transmission.packet.property.communicator.CommunicationEndpoint;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public
class TestPendingPacketDAO {

    @Test
    void writePackets ()
    throws InterruptedException {
        PacketCompiler.addOriginatorEntityProvider(this::getOriginator);
        PacketCompiler.addContentIdentificator(
                new ContentIdentificationProviderImpl());
        var dao = new PendingPacketDAO();
        dao.createFileAccess(18002);
        dao.setPendingPackets(PendingType.PROCESSOR_BOUND, Collections.emptyMap());

        List<PacketContent> allPacketContentSamples = new ArrayList<>();
        final var authData = AuthenticationDTO.valueOf("login", "password");
        final var communicatorData = CommunicatorData.valueOf(15001, 15001, "test name");
        final var commDTO = ConvertCommDataToDTO.convertFrom(communicatorData);
        allPacketContentSamples.add(new LoginRequestDTO(authData));
        allPacketContentSamples.add(new LoginResponseDTO(commDTO));
        allPacketContentSamples.add(new LogoutRequestDTO(commDTO));
        allPacketContentSamples.add(new LogoutResponseDTO(true));
        allPacketContentSamples.add(new ReconnectRequestDTO(commDTO));
        allPacketContentSamples.add(new ReconnectResponseDTO(true));
        allPacketContentSamples.add(new NewAccountRequestDTO());
        allPacketContentSamples.add(new ClientStatusChangeDTO(ClientService.MESSENGER, true, commDTO));
        allPacketContentSamples.add(new MessengerSetupDTO(Collections.emptyMap(), Collections.emptyMap()));
        allPacketContentSamples.add(new MessengerTearDownDTO(true));
        allPacketContentSamples.add(new ContactMessengerStatusDTO(EligibleContactEntity.CLIENT, true, commDTO, Collections.emptyList()));
        final var relationRequest = new ContactRelationRequestDTO(EligibleContactEntity.CLIENT, commDTO.getCommunicatorId(), 15002, commDTO, true);
        allPacketContentSamples.add(relationRequest);
        allPacketContentSamples.add(ContactRelationResponseDTO.valueOf(relationRequest, commDTO, true));
        allPacketContentSamples.add(new ErrorDTO("error", null));
        allPacketContentSamples.add(new TextMessageDTO(commDTO.getCommunicatorId(), EligibleContactEntity.CLIENT, 15002, "test nachricht"));
        allPacketContentSamples.add(new SimpleStatusSyncDTO((SimpleStatusSyncBuilder)new SimpleStatusSyncBuilder<>().withToAdd(true).withClientState(
                ClientState.ACTIVE_MESSENGER).withContactData(commDTO).withOriginatingServerId(2134)));
        allPacketContentSamples.add(new ExtendedStatusSyncDTO((ExtendedStatusSyncBuilder)new ExtendedStatusSyncBuilder<>().withContactIdSet(Collections.emptySet()).withClientState(ClientState.ACTIVE_MESSENGER).withContactData(commDTO).withOriginatingServerId(1234)));
        allPacketContentSamples.add(new ServerFailureDTO((ServerFailureContentBuilder)new ServerFailureContentBuilder().withFailedServerId(4234).withOriginatingServerId(4234)));
        allPacketContentSamples.add(new InterServerCommSyncDTO(1234));


        for(var content : allPacketContentSamples){
            final var toWrite = PacketCompiler.createRequest(CommunicationEndpoint.getClientEntity(15002),content);
            dao.appendPendingPacket(PendingType.PROCESSOR_BOUND, toWrite);
        }
        final var pendingProcessorBound = dao.readPendingPackets(PendingType.PROCESSOR_BOUND);
        Assertions.assertEquals(allPacketContentSamples.size(), pendingProcessorBound.size());
    }

    CommunicationEndpoint getOriginator () {
        return CommunicationEndpoint.getClientEntity(15001);
    }
}
