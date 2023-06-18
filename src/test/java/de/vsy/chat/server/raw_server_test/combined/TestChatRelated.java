package de.vsy.chat.server.raw_server_test.combined;

import de.vsy.chat.server.raw_server_test.AuthenticationHelper;
import de.vsy.chat.server.raw_server_test.ServerPortProvider;
import de.vsy.chat.server.raw_server_test.ServerTestBase;
import de.vsy.chat.server.raw_server_test.StatusChangeHelper;
import de.vsy.chat.server.server_test_helpers.ClientConnection;
import de.vsy.shared_transmission.dto.authentication.AuthenticationDTO;
import de.vsy.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.packet.content.PacketContent;
import de.vsy.shared_transmission.packet.content.authentication.ReconnectRequestDTO;
import de.vsy.shared_transmission.packet.content.authentication.ReconnectResponseDTO;
import de.vsy.shared_transmission.packet.content.chat.TextMessageDTO;
import de.vsy.shared_transmission.packet.content.relation.EligibleContactEntity;
import de.vsy.shared_transmission.packet.content.status.ContactStatusChangeDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static de.vsy.chat.server.raw_server_test.TestClientDataProvider.FRANK_1_AUTH;
import static de.vsy.chat.server.raw_server_test.TestClientDataProvider.MARKUS_1_AUTH;
import static de.vsy.chat.server.server_test_helpers.TestPacketVerifier.verifyPacketContent;
import static de.vsy.shared_transmission.packet.property.communicator.CommunicationEndpoint.getClientEntity;
import static de.vsy.shared_transmission.packet.property.communicator.CommunicationEndpoint.getServerEntity;
import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_SERVER_ID;

public class TestChatRelated extends ServerTestBase {
    Logger LOGGER = LogManager.getLogger();

    public TestChatRelated(ServerPortProvider clientConnectionPorts, List<AuthenticationDTO> clientAuthenticationDataList) {
        super(clientConnectionPorts, clientAuthenticationDataList);
    }


    @Test
    void testClientMessageAfterReconnect() throws IOException, InterruptedException {
        LOGGER.info("Test: message reception after reconnect -> success");
        PacketContent content;
        Packet receivedPacket, responsePacket;
        final ClientConnection clientOne, clientTwo;
        final int contactId;

        super.addConnectionNextServer();
        clientOne = getUnusedClientConnection();
        AuthenticationHelper.loginSpecificClient(clientOne, FRANK_1_AUTH);
        var clientOneCommunicatorData = clientOne.getCommunicatorData();
        var clientOneCredentials = clientOne.getAuthenticationData();
        clientTwo = getUnusedClientConnection();
        AuthenticationHelper.loginSpecificClient(clientTwo, MARKUS_1_AUTH);

        StatusChangeHelper.changeMessengerStatus(clientOne, true);
        StatusChangeHelper.changeMessengerStatus(clientTwo, true);
        Packet contactStatus = clientOne.readPacket();
        verifyPacketContent(contactStatus, ContactStatusChangeDTO.class);

        contactId = clientTwo.getCommunicatorData().getCommunicatorId();

        final var message = new TextMessageDTO(clientOne.getCommunicatorData().getCommunicatorId(),
                EligibleContactEntity.CLIENT, contactId, "test message");
        clientOne.sendRequest(message, getClientEntity(contactId));

        clientOne.resetConnection();
        Thread.sleep(500);
        content = new ReconnectRequestDTO(clientOneCommunicatorData);
        clientOne.sendRequest(content, getServerEntity(STANDARD_SERVER_ID));
        var response = clientOne.readPacket();
        verifyPacketContent(response, ReconnectResponseDTO.class);
        clientOne.setClientData(clientOneCredentials, clientOneCommunicatorData);

        receivedPacket = clientTwo.readPacket();
        responsePacket = clientOne.readPacket();
        verifyPacketContent(receivedPacket, TextMessageDTO.class);
        verifyPacketContent(responsePacket, TextMessageDTO.class);

        final var receivedMessage = (TextMessageDTO) receivedPacket.getPacketContent();
        final var responseMessage = (TextMessageDTO) responsePacket.getPacketContent();
        Assertions.assertEquals(message.getMessage(), receivedMessage.getMessage(), responseMessage.getMessage());
        LOGGER.info("Test: message reception after reconnect -> success -- terminated");
    }
}
