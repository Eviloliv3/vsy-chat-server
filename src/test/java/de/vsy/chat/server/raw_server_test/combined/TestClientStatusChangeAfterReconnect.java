package de.vsy.chat.server.raw_server_test.combined;

import de.vsy.chat.server.raw_server_test.ServerPortProvider;
import de.vsy.chat.server.raw_server_test.ServerTestBase;
import de.vsy.chat.server.server_test_helpers.ClientConnection;
import de.vsy.shared_transmission.dto.authentication.AuthenticationDTO;
import de.vsy.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.packet.content.PacketContent;
import de.vsy.shared_transmission.packet.content.authentication.ReconnectRequestDTO;
import de.vsy.shared_transmission.packet.content.authentication.ReconnectResponseDTO;
import de.vsy.shared_transmission.packet.content.status.ClientStatusChangeDTO;
import de.vsy.shared_transmission.packet.content.status.ContactStatusChangeDTO;
import de.vsy.shared_transmission.packet.content.status.MessengerSetupDTO;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static de.vsy.chat.server.raw_server_test.StatusChangeHelper.changeMessengerStatus;
import static de.vsy.chat.server.server_test_helpers.TestPacketVerifier.verifyPacketContent;
import static de.vsy.chat.server.server_test_helpers.TestResponseSingleClient.checkResponse;
import static de.vsy.shared_transmission.packet.content.status.ClientService.MESSENGER;
import static de.vsy.shared_transmission.packet.property.communicator.CommunicationEndpoint.getServerEntity;
import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_SERVER_ID;

public class TestClientStatusChangeAfterReconnect extends ServerTestBase {

    public TestClientStatusChangeAfterReconnect(ServerPortProvider clientConnectionPorts, List<AuthenticationDTO> clientAuthenticationDataList) {
        super(clientConnectionPorts, clientAuthenticationDataList);
    }

    @Test
    void testMessengerSetupAfterClientReconnect() throws IOException, InterruptedException {
        LOGGER.info("Test: client messenger status set -> success");
        Packet response;
        PacketContent content;
        ClientConnection clientOneReconnection;
        final var clientOne = super.loginNextClient();
        final var clientOneCredentials = clientOne.getAuthenticationData();
        final var clientOneCommunicatorDTO = clientOne.getCommunicatorData();
        super.addConnectionNextServer();

        content = new ClientStatusChangeDTO(MESSENGER, true, clientOne.getCommunicatorData());
        clientOne.sendRequest(content, getServerEntity(STANDARD_SERVER_ID));
        clientOne.resetConnection();
        Thread.sleep(500);

        clientOneReconnection = super.getUnusedClientConnection();
        clientOneReconnection.setClientData(clientOneCredentials, clientOneCommunicatorDTO);
        content = new ReconnectRequestDTO(clientOneCommunicatorDTO);
        checkResponse(clientOneReconnection, getServerEntity(STANDARD_SERVER_ID), content, ReconnectResponseDTO.class);

        response = clientOneReconnection.readPacket();
        verifyPacketContent(response, MessengerSetupDTO.class);
        LOGGER.info("Test: client messenger status set -> success -- terminated");
    }

    @Test
    void testContactStatusChangeAfterClientReconnect() throws IOException, InterruptedException {
        LOGGER.info("Test: client messenger status set -> success");
        Packet response;
        PacketContent content;
        final var clientOne = super.loginNextClient();
        super.addConnectionNextServer();
        final var clientTwo = super.loginNextClient();
        super.addConnectionNextServer();
        final var clientOneReconnection = super.getUnusedClientConnection();
        final var clientOneCredentials = clientOne.getAuthenticationData();
        final var clientOneCommunicatorDTO = clientOne.getCommunicatorData();

        changeMessengerStatus(clientOne, true);
        content = new ClientStatusChangeDTO(MESSENGER, true, clientTwo.getCommunicatorData());
        clientTwo.sendRequest(content, getServerEntity(STANDARD_SERVER_ID));
        clientOne.resetConnection();
        Thread.sleep(500);

        clientOneReconnection.setClientData(clientOneCredentials, clientOneCommunicatorDTO);
        content = new ReconnectRequestDTO(clientOneCommunicatorDTO);
        checkResponse(clientOneReconnection, getServerEntity(STANDARD_SERVER_ID), content, ReconnectResponseDTO.class);

        response = clientOneReconnection.readPacket();
        verifyPacketContent(response, ContactStatusChangeDTO.class);
        LOGGER.info("Test: client messenger status set -> success -- terminated");
    }
}
