package de.vsy.chat.server.raw_server_test.chat;

import de.vsy.chat.server.raw_server_test.ServerPortProvider;
import de.vsy.chat.server.raw_server_test.ServerTestBase;
import de.vsy.chat.server.server_test_helpers.ClientConnection;
import de.vsy.shared_transmission.dto.authentication.AuthenticationDTO;
import de.vsy.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.packet.content.PacketContent;
import de.vsy.shared_transmission.packet.content.chat.TextMessageDTO;
import de.vsy.shared_transmission.packet.content.relation.EligibleContactEntity;
import de.vsy.shared_transmission.packet.content.status.ClientStatusChangeDTO;
import de.vsy.shared_transmission.packet.content.status.MessengerSetupDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static de.vsy.chat.server.raw_server_test.TestClientDataProvider.*;
import static de.vsy.chat.server.server_test_helpers.TestPacketVerifier.verifyPacketContent;
import static de.vsy.chat.server.server_test_helpers.TestResponseSingleClient.checkErrorResponse;
import static de.vsy.chat.server.server_test_helpers.TestResponseSingleClient.checkResponse;
import static de.vsy.shared_transmission.packet.content.status.ClientService.MESSENGER;
import static de.vsy.shared_transmission.packet.property.communicator.CommunicationEndpoint.getClientEntity;
import static de.vsy.shared_transmission.packet.property.communicator.CommunicationEndpoint.getServerEntity;
import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_SERVER_ID;

public class TestClientMessage extends ServerTestBase {

    public TestClientMessage(final ServerPortProvider clientConnectionPorts,
                             final List<AuthenticationDTO> clientAuthenticationDataList) {
        super(clientConnectionPorts, clientAuthenticationDataList);
    }

    @Test
    void sendMessageContactNotActiveFail() throws IOException {
        LOGGER.info("Test: send message -> failure: contact offline");
        final ClientConnection clientOne, clientTwo;

        clientOne = super.loginNextClient();
        super.addConnectionNextServer();
        clientTwo = super.loginNextClient();

        changeStatus(clientOne, true);

        final var inactiveClientId = clientTwo.getCommunicatorData().getCommunicatorId();

        final var message = new TextMessageDTO(clientOne.getCommunicatorData().getCommunicatorId(),
                EligibleContactEntity.CLIENT, inactiveClientId, "test message");

        checkErrorResponse(clientOne, getClientEntity(inactiveClientId), message,
                "Packet could not be delivered. Contact offline.");
        LOGGER.info("Test: send message -> failure: contact offline -- terminated");
    }

    private void changeStatus(ClientConnection connection, boolean changeTo) {
        PacketContent content = new ClientStatusChangeDTO(MESSENGER, changeTo,
                connection.getCommunicatorData());
        checkResponse(connection, getServerEntity(STANDARD_SERVER_ID), content,
                MessengerSetupDTO.class);
    }

    @Test
    void sendMessageNoContactFail() throws IOException {
        LOGGER.info("Test: send message -> not a contact");
        Packet contactStatus;
        final ClientConnection clientOne, clientTwo;
        final int noContactId;

        super.addConnectionNextServer();
        clientOne = loginSpecificClient(FRANK_1_AUTH);
        clientTwo = loginSpecificClient(THOMAS_1_AUTH);

        Assertions.assertNotNull(clientOne);
        Assertions.assertNotNull(clientTwo);

        changeStatus(clientOne, true);
        changeStatus(clientTwo, true);

        noContactId = clientTwo.getCommunicatorData().getCommunicatorId();

        final var message = new TextMessageDTO(clientOne.getCommunicatorData().getCommunicatorId(),
                EligibleContactEntity.CLIENT, noContactId, "test message");

        checkErrorResponse(clientOne, getClientEntity(noContactId), message,
                "Text message was not delivered. 15006 is no contact of yours.");
        LOGGER.info("Test: send message -> not a contact -- terminated");
    }

    private ClientConnection loginSpecificClient(final AuthenticationDTO credentials) {
        final var connection = super.getUnusedClientConnection();

        if (connection != null) {
            connection.setClientData(credentials, null);
            if (connection.tryClientLogin()) {
                return connection;
            } else {
                connection.setClientData(null, null);
            }
        } else {
            Assertions.fail("No usable connection.");
        }
        return null;
    }

    @Test
    void sendMessageSuccess() throws IOException {
        LOGGER.info("Test: send message -> success");
        Packet receivedPacket, responsePacket;
        final ClientConnection clientOne, clientTwo;
        final int contactId;

        super.addConnectionNextServer();
        clientOne = loginSpecificClient(FRANK_1_AUTH);
        clientTwo = loginSpecificClient(MARKUS_1_AUTH);

        changeStatus(clientOne, true);
        changeStatus(clientTwo, true);
        Packet contactStatus = clientOne.readPacket();

        contactId = clientTwo.getCommunicatorData().getCommunicatorId();

        final var message = new TextMessageDTO(clientOne.getCommunicatorData().getCommunicatorId(),
                EligibleContactEntity.CLIENT, contactId, "test message");
        clientOne.sendRequest(message, getClientEntity(contactId));

        receivedPacket = clientTwo.readPacket();
        responsePacket = clientOne.readPacket();
        verifyPacketContent(receivedPacket, TextMessageDTO.class);
        verifyPacketContent(responsePacket, TextMessageDTO.class);

        final var receivedMessage = (TextMessageDTO) receivedPacket.getPacketContent();
        final var responseMessage = (TextMessageDTO) responsePacket.getPacketContent();
        Assertions.assertEquals(message.getMessage(), receivedMessage.getMessage(), responseMessage.getMessage());
        LOGGER.info("Test: send message -> success -- terminated");
    }
}
