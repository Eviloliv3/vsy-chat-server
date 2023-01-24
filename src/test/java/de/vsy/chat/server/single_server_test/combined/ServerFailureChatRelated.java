package de.vsy.chat.server.single_server_test.combined;

import de.vsy.chat.server.raw_server_test.AuthenticationHelper;
import de.vsy.chat.server.raw_server_test.StatusChangeHelper;
import de.vsy.chat.server.server_test_helpers.ClientConnection;
import de.vsy.shared_transmission.dto.CommunicatorDTO;
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

import static de.vsy.chat.server.raw_server_test.TestClientDataProvider.FRANK_1_AUTH;
import static de.vsy.chat.server.raw_server_test.TestClientDataProvider.MARKUS_1_AUTH;
import static de.vsy.chat.server.server_test_helpers.TestPacketVerifier.verifyPacketContent;
import static de.vsy.shared_transmission.packet.property.communicator.CommunicationEndpoint.getClientEntity;
import static de.vsy.shared_transmission.packet.property.communicator.CommunicationEndpoint.getServerEntity;
import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_SERVER_ID;

public class ServerFailureChatRelated {

    Logger LOGGER = LogManager.getLogger();

    @Test
    void testMessageAfterServerFailureSuccess() throws IOException, InterruptedException {
        LOGGER.info("Test: send message -> success");
        PacketContent content;
        Packet receivedPacket, responsePacket;
        ClientConnection clientOne, clientTwo, aliveConnection, disconnectedClient;
        CommunicatorDTO disconnectedClientCommunicatorData;
        AuthenticationDTO disconnectedClientCredentials;
        final int contactId;
        Thread.sleep(1000);

        clientOne = new ClientConnection(7371);
        AuthenticationHelper.loginSpecificClient(clientOne, FRANK_1_AUTH);
        clientTwo = new ClientConnection(7370);
        AuthenticationHelper.loginSpecificClient(clientTwo, MARKUS_1_AUTH);

        StatusChangeHelper.changeStatus(clientOne, true);
        StatusChangeHelper.changeStatus(clientTwo, true);

        Packet contactStatus = clientOne.readPacket();
        verifyPacketContent(contactStatus, ContactStatusChangeDTO.class);

        LOGGER.error("Kill 7370 Server now.");
        Thread.sleep(3000);

        if (7370 == clientOne.getServerPort()) {
            aliveConnection = clientTwo;
            disconnectedClient = clientOne;
        } else {
            aliveConnection = clientOne;
            disconnectedClient = clientTwo;
        }
        disconnectedClientCommunicatorData = disconnectedClient.getCommunicatorData();
        disconnectedClientCredentials = disconnectedClient.getAuthenticationData();
        contactId = disconnectedClientCommunicatorData.getCommunicatorId();
        Thread.sleep(500);

        final var message = new TextMessageDTO(aliveConnection.getCommunicatorData().getCommunicatorId(),
                EligibleContactEntity.CLIENT, contactId, "test message");
        aliveConnection.sendRequest(message, getClientEntity(contactId));

        disconnectedClient = new ClientConnection(7371);
        content = new ReconnectRequestDTO(disconnectedClientCommunicatorData);
        disconnectedClient.sendRequest(content, getServerEntity(STANDARD_SERVER_ID));
        var response = disconnectedClient.readPacket();
        verifyPacketContent(response, ReconnectResponseDTO.class);
        disconnectedClient.setClientData(disconnectedClientCredentials, disconnectedClientCommunicatorData);

        receivedPacket = disconnectedClient.readPacket();
        responsePacket = aliveConnection.readPacket();
        verifyPacketContent(receivedPacket, TextMessageDTO.class);
        verifyPacketContent(responsePacket, TextMessageDTO.class);

        final var receivedMessage = (TextMessageDTO) receivedPacket.getPacketContent();
        final var responseMessage = (TextMessageDTO) responsePacket.getPacketContent();
        Assertions.assertEquals(message.getMessage(), receivedMessage.getMessage(), responseMessage.getMessage());
        LOGGER.info("Test: send message -> success -- terminated");
    }
}
