package de.vsy.chat.server.raw_server_test.status;

import de.vsy.chat.server.raw_server_test.ServerPortProvider;
import de.vsy.chat.server.raw_server_test.ServerTestBase;
import de.vsy.chat.server.server_test_helpers.ClientConnection;
import de.vsy.shared_transmission.dto.CommunicatorDTO;
import de.vsy.shared_transmission.dto.authentication.AuthenticationDTO;
import de.vsy.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.packet.content.PacketContent;
import de.vsy.shared_transmission.packet.content.status.ClientStatusChangeDTO;
import de.vsy.shared_transmission.packet.content.status.ContactStatusChangeDTO;
import de.vsy.shared_transmission.packet.content.status.MessengerSetupDTO;
import de.vsy.shared_transmission.packet.content.status.MessengerTearDownDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static de.vsy.chat.server.server_test_helpers.TestPacketVerifier.verifyPacketContent;
import static de.vsy.chat.server.server_test_helpers.TestResponseSingleClient.checkErrorResponse;
import static de.vsy.chat.server.server_test_helpers.TestResponseSingleClient.checkResponse;
import static de.vsy.shared_transmission.packet.content.status.ClientService.MESSENGER;
import static de.vsy.shared_transmission.packet.property.communicator.CommunicationEndpoint.getServerEntity;
import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_SERVER_ID;

public class TestStatusChange extends ServerTestBase {

    public TestStatusChange(final ServerPortProvider clientConnectionPorts,
                            final List<AuthenticationDTO> clientAuthenticationDataList) {
        super(clientConnectionPorts, clientAuthenticationDataList);
    }

    @Test
    public void changeFromMessengerStateTwoClientSuccess() throws IOException, InterruptedException {
        LOGGER.info("Test: change status for two clients that are contacts -> success");
        Packet packet;
        PacketContent content;
        final ClientConnection clientOne, clientTwo;
        final CommunicatorDTO clientOneCommunicatorDTO;

        super.addConnectionNextServer();
        clientOne = super.loginNextClient();
        clientOneCommunicatorDTO = clientOne.getCommunicatorData();
        clientTwo = super.loginNextClient();

        content = new ClientStatusChangeDTO(MESSENGER, true, clientOne.getCommunicatorData());
        checkResponse(clientOne, getServerEntity(STANDARD_SERVER_ID), content, MessengerSetupDTO.class);

        content = new ClientStatusChangeDTO(MESSENGER, true, clientTwo.getCommunicatorData());
        checkResponse(clientTwo, getServerEntity(STANDARD_SERVER_ID), content, MessengerSetupDTO.class);

        packet = clientOne.readPacket();
        LOGGER.info("Expecting ContactStatusChangeDTO. Read: {}", packet);

        Assertions.assertTrue(clientOne.tryClientLogout(), "Logout failed for clientOne.");
        packet = clientTwo.readPacket();

        verifyPacketContent(packet, ContactStatusChangeDTO.class);
        final var contactStatus = (ContactStatusChangeDTO) packet.getPacketContent();
        Assertions.assertTrue(contactStatus.getContactData().equals(clientOneCommunicatorDTO));
        Assertions.assertFalse(contactStatus.getOnlineStatus());
        clientOne.resetConnection();
        LOGGER.info("Test: change status for two clients that are contacts -> success -- terminated");
    }

    @Test
    public void changeFromMessengerStateSingleClientSuccess() {
        LOGGER.info("Test: client messenger status removal -> success");
        Packet packet;
        PacketContent content;
        ClientConnection clientOne;
        clientOne = super.loginNextClient();

        content = new ClientStatusChangeDTO(MESSENGER, true, clientOne.getCommunicatorData());
        clientOne.sendRequest(content, getServerEntity(STANDARD_SERVER_ID));
        packet = clientOne.readPacket();
        verifyPacketContent(packet, MessengerSetupDTO.class);
        content = new ClientStatusChangeDTO(MESSENGER, false, clientOne.getCommunicatorData());
        clientOne.sendRequest(content, getServerEntity(STANDARD_SERVER_ID));
        packet = clientOne.readPacket();
        verifyPacketContent(packet, MessengerTearDownDTO.class);
        LOGGER.info("Test: client messenger status removal -> success -- terminated");
    }

    @Test
    public void changeToMessengerStateSingleClientSuccess() {
        LOGGER.info("Test: client messenger status set -> success");
        PacketContent content;
        final var clientOne = super.loginNextClient();

        content = new ClientStatusChangeDTO(MESSENGER, true, clientOne.getCommunicatorData());
        checkResponse(clientOne, getServerEntity(STANDARD_SERVER_ID), content, MessengerSetupDTO.class);
        LOGGER.info("Test: client messenger status set -> success -- terminated");
    }

    @Test
    public void changeMessengerNoStateFailed() {
        LOGGER.info("Test: client messenger status change -> failure: no desired status transmitted");
        ClientStatusChangeDTO content;
        final var clientOne = super.loginNextClient();

        content = new ClientStatusChangeDTO(null, true, clientOne.getCommunicatorData());
        checkErrorResponse(clientOne, getServerEntity(STANDARD_SERVER_ID), content,
                "No service type specified.");
        LOGGER.info(
                "Test: client messenger status change -> failure: no desired status transmitted -- terminated");
    }

    @Test
    public void changeToMessengerStateTwoClientSuccess() throws IOException {
        LOGGER.info("Test: change messenger status for two clients that are contacts -> success");
        Packet packet;
        PacketContent content;
        final ClientConnection clientOne, clientTwo;

        super.addConnectionNextServer();
        clientOne = super.loginNextClient();
        clientTwo = super.loginNextClient();

        content = new ClientStatusChangeDTO(MESSENGER, true, clientOne.getCommunicatorData());
        checkResponse(clientOne, getServerEntity(STANDARD_SERVER_ID), content, MessengerSetupDTO.class);
        LOGGER.info("MessengerSetup 1 read");
        content = new ClientStatusChangeDTO(MESSENGER, true, clientTwo.getCommunicatorData());
        checkResponse(clientTwo, getServerEntity(STANDARD_SERVER_ID), content, MessengerSetupDTO.class);
        LOGGER.info("MessengerSetup 2 read");

        packet = clientOne.readPacket();
        LOGGER.info("Expecting ContactStatusChangeDTO. Read: {}", packet);
        verifyPacketContent(packet, ContactStatusChangeDTO.class);
        Assertions.assertEquals(((ContactStatusChangeDTO) packet.getPacketContent()).getContactData(), clientTwo.getCommunicatorData());
        LOGGER.info(
                "Test: change messenger status for two clients that are contacts -> success -- terminated");
    }
}
