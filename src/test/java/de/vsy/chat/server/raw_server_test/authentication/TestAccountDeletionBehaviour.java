package de.vsy.chat.server.raw_server_test.authentication;

import de.vsy.chat.server.raw_server_test.ServerPortProvider;
import de.vsy.chat.server.raw_server_test.ServerTestBase;
import de.vsy.chat.server.server_test_helpers.ClientConnection;
import de.vsy.shared_transmission.dto.CommunicatorDTO;
import de.vsy.shared_transmission.dto.authentication.AuthenticationDTO;
import de.vsy.shared_transmission.dto.authentication.PersonalData;
import de.vsy.shared_transmission.dto.builder.AccountCreationDTOBuilder;
import de.vsy.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.packet.content.PacketContent;
import de.vsy.shared_transmission.packet.content.authentication.AccountCreationRequestDTO;
import de.vsy.shared_transmission.packet.content.authentication.AccountDeletionRequestDTO;
import de.vsy.shared_transmission.packet.content.authentication.AccountDeletionResponseDTO;
import de.vsy.shared_transmission.packet.content.authentication.LoginResponseDTO;
import de.vsy.shared_transmission.packet.content.relation.ContactRelationRequestDTO;
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
import static de.vsy.shared_transmission.packet.property.communicator.CommunicationEndpoint.getServerEntity;
import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_SERVER_ID;

public class TestAccountDeletionBehaviour extends ServerTestBase {
    public TestAccountDeletionBehaviour(ServerPortProvider clientConnectionPorts, List<AuthenticationDTO> clientAuthenticationDataList) {
        super(clientConnectionPorts, clientAuthenticationDataList);
    }
/*
    @Test
    void deletionFailureNotLoggedIn() {
        LOGGER.info("Test: deletion -> failure: not authenticated");
        final var clientOne = super.getUnusedClientConnection();
        final var content = new AccountDeletionRequestDTO();
        checkErrorResponse(clientOne, getServerEntity(STANDARD_SERVER_ID), content, "Request not processed. You are not authenticated.");
        LOGGER.info("Test: deletion -> failure: not authenticated -- terminated");
    }

 */

    @Test
    void deletionSuccessContactNotification() throws IOException {
        LOGGER.info("Test: contact notification after deletion -> success");
        Packet receivedPacket = null;
        PacketContent content = null;
        final var clientOne = super.getUnusedClientConnection();
        loginClient(clientOne, HARALD_1_AUTH);
        super.addConnectionNextServer();
        final var clientTwo = super.getUnusedClientConnection();
        loginClient(clientTwo, GERALD_1_AUTH);

        content = new ClientStatusChangeDTO(MESSENGER, true, clientOne.getCommunicatorData());
        checkResponse(clientOne, getServerEntity(STANDARD_SERVER_ID), content, MessengerSetupDTO.class);

        content = new ClientStatusChangeDTO(MESSENGER, true, clientTwo.getCommunicatorData());
        checkResponse(clientTwo, getServerEntity(STANDARD_SERVER_ID), content, MessengerSetupDTO.class);

        do {
            receivedPacket = clientOne.readPacket();
            LOGGER.info("Discarding clientOne Packet: {}", receivedPacket);
        } while (receivedPacket != null);

        do {
            receivedPacket = clientTwo.readPacket();
            LOGGER.info("Discarding clientTwo Packet: {}", receivedPacket);
        } while (receivedPacket != null);
        content = new AccountDeletionRequestDTO();
        checkResponse(clientOne, getServerEntity(STANDARD_SERVER_ID), content, AccountDeletionResponseDTO.class);

        receivedPacket = clientTwo.readPacket();
        verifyPacketContent(receivedPacket, ContactRelationRequestDTO.class);
        LOGGER.info("Test: contact notification after deletion -> success -- terminated");
    }

 /*


    @Test
    void deletionSuccess() {
        LOGGER.info("Test: deletion -> success");
        final var clientOne = super.getUnusedClientConnection();
        loginClient(clientOne, MARK_1_AUTH);
        PacketContent content = new AccountDeletionRequestDTO();
        checkResponse(clientOne, getServerEntity(STANDARD_SERVER_ID), content, AccountDeletionResponseDTO.class);
        LOGGER.info("Test: deletion -> success -- terminated");
    }

 */

    private void loginClient(final ClientConnection connection, final AuthenticationDTO credentials){
        connection.setClientData(credentials, null);
        Assertions.assertTrue(connection.tryClientLogin(), "Login failed for: " + credentials);
    }
}
