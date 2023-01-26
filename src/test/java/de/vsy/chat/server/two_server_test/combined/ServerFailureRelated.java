package de.vsy.chat.server.two_server_test.combined;

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
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static de.vsy.chat.server.raw_server_test.TestClientDataProvider.FRANK_1_AUTH;
import static de.vsy.chat.server.raw_server_test.TestClientDataProvider.MARKUS_1_AUTH;
import static de.vsy.chat.server.server_test_helpers.TestPacketVerifier.verifyPacketContent;
import static de.vsy.shared_transmission.packet.property.communicator.CommunicationEndpoint.getClientEntity;
import static de.vsy.shared_transmission.packet.property.communicator.CommunicationEndpoint.getServerEntity;
import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_SERVER_ID;
import static de.vsy.shared_utility.standard_value.ThreadContextValues.LOG_FILE_CONTEXT_KEY;
import static de.vsy.shared_utility.standard_value.ThreadContextValues.LOG_ROUTE_CONTEXT_KEY;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ServerFailureRelated {
    Logger LOGGER = LogManager.getLogger();
    List<ClientConnection> connections;

    public ServerFailureRelated(){

    }

    @BeforeEach
void initConnectionList(){
        ThreadContext.put(LOG_ROUTE_CONTEXT_KEY, "test");
        ThreadContext.put(LOG_FILE_CONTEXT_KEY, "serverFailureRelated");
    connections = new LinkedList<>();
}

@AfterEach
void clearConnections() throws InterruptedException {
        System.out.println("Removing all connections.");
        for(final var connection : connections){
            AuthenticationHelper.logoutClient(connection);
        }
        connections.clear();
        ThreadContext.clearAll();
        Thread.sleep(500);
}

    @Test
    void testMessage() throws IOException, InterruptedException {
        LOGGER.info("Test: send message -> success");
        PacketContent content;
        Packet receivedPacket, responsePacket;
        ClientConnection clientOne, clientTwo, aliveConnection, disconnectedClient;
        CommunicatorDTO disconnectedClientCommunicatorData;
        AuthenticationDTO disconnectedClientCredentials;
        final int contactId;

        clientOne = createConnection(7371);
        AuthenticationHelper.loginSpecificClient(clientOne, FRANK_1_AUTH);
        clientTwo = createConnection(7370);
        AuthenticationHelper.loginSpecificClient(clientTwo, MARKUS_1_AUTH);

        StatusChangeHelper.changeMessengerStatus(clientOne, true);
        StatusChangeHelper.changeMessengerStatus(clientTwo, true);

        Packet contactStatus = clientOne.readPacket();
        verifyPacketContent(contactStatus, ContactStatusChangeDTO.class);

        LOGGER.error("Kill 7370 server now.");
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

        final var message = new TextMessageDTO(aliveConnection.getCommunicatorData().getCommunicatorId(),
                EligibleContactEntity.CLIENT, contactId, "test message");
        aliveConnection.sendRequest(message, getClientEntity(contactId));

        disconnectedClient = reconnectClient(7371, disconnectedClientCredentials, disconnectedClientCommunicatorData);

        receivedPacket = disconnectedClient.readPacket();
        verifyPacketContent(receivedPacket, TextMessageDTO.class);
        responsePacket = aliveConnection.readPacket();
        verifyPacketContent(responsePacket, ContactStatusChangeDTO.class);
        responsePacket = aliveConnection.readPacket();
        verifyPacketContent(responsePacket, TextMessageDTO.class);

        final var receivedMessage = (TextMessageDTO) receivedPacket.getPacketContent();
        final var responseMessage = (TextMessageDTO) responsePacket.getPacketContent();
        Assertions.assertEquals(message.getMessage(), receivedMessage.getMessage(), responseMessage.getMessage());
        LOGGER.info("Test: send message -> success -- terminated");
    }

    ClientConnection reconnectClient(int port, AuthenticationDTO disconnectedClientCredentials, CommunicatorDTO disconnectedClientCommunicatorData) throws IOException {
        var newConnection = createConnection(port);
        var reconnectContent = new ReconnectRequestDTO(disconnectedClientCommunicatorData);
        newConnection.sendRequest(reconnectContent, getServerEntity(STANDARD_SERVER_ID));
        var response = newConnection.readPacket();
        verifyPacketContent(response, ReconnectResponseDTO.class);
        newConnection.setClientData(disconnectedClientCredentials, disconnectedClientCommunicatorData);
        return newConnection;
    }
    ClientConnection createConnection(int serverPort) throws IOException {
        var newConnection = new ClientConnection(serverPort);
        this.connections.add(newConnection);
        return newConnection;
    }
}
