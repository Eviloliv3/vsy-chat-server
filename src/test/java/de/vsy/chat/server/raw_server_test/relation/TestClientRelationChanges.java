package de.vsy.chat.server.raw_server_test.relation;

import static de.vsy.chat.server.raw_server_test.TestClientDataProvider.ADRIAN_1_COMM;
import static de.vsy.chat.server.raw_server_test.TestClientDataProvider.MARKUS_1_COMM;
import static de.vsy.chat.server.raw_server_test.TestClientDataProvider.MAX_1_COMM;
import static de.vsy.chat.server.raw_server_test.TestClientDataProvider.PETER_1_COMM;
import static de.vsy.shared_transmission.packet.property.communicator.CommunicationEndpoint.getClientEntity;
import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_SERVER_ID;

import de.vsy.chat.server.raw_server_test.ServerPortProvider;
import de.vsy.chat.server.raw_server_test.ServerTestBase;
import de.vsy.chat.server.raw_server_test.TestClientDataProvider;
import de.vsy.chat.server.server_test_helpers.ClientConnection;
import de.vsy.chat.server.server_test_helpers.TestResponseSingleClient;
import de.vsy.shared_transmission.dto.CommunicatorDTO;
import de.vsy.shared_transmission.dto.authentication.AuthenticationDTO;
import de.vsy.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.packet.content.PacketContent;
import de.vsy.shared_transmission.packet.content.relation.ContactRelationRequestDTO;
import de.vsy.shared_transmission.packet.content.relation.ContactRelationResponseDTO;
import de.vsy.shared_transmission.packet.content.relation.EligibleContactEntity;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestClientRelationChanges extends ServerTestBase {

  public TestClientRelationChanges(final ServerPortProvider clientConnectionPorts,
      final List<AuthenticationDTO> clientAuthenticationDataList) {
    super(clientConnectionPorts, clientAuthenticationDataList);
  }

  @Test
  void addContactSuccess() throws IOException {
    LOGGER.info("Test: add contact -> success");
    Packet packet;
    PacketContent content;
    final ClientConnection clientOne, clientTwo;
    final CommunicatorDTO clientOneData, clientTwoData;

    clientOne = super.loginNextClient();

    super.addConnectionNextServer();
    clientTwo = super.getUnusedClientConnection();
    clientTwo.setClientData(TestClientDataProvider.ADRIAN_1_AUTH, null);
    Assertions.assertTrue(clientTwo.tryClientLogin(), "Login failed.");

    clientOneData = clientOne.getCommunicatorData();
    clientTwoData = clientTwo.getCommunicatorData();

    content = new ContactRelationRequestDTO(EligibleContactEntity.CLIENT,
        clientOneData.getCommunicatorId(),
        ADRIAN_1_COMM.getCommunicatorId(), clientOneData, true);

    clientOne.sendRequest(content, getClientEntity(ADRIAN_1_COMM.getCommunicatorId()));
    packet = clientTwo.readPacket();

    if (packet == null || !(packet.getPacketContent() instanceof ContactRelationRequestDTO)) {
      Assertions.fail("No response for ContactRelationRequestDTO received.");
    } else {
      content = packet.getPacketContent();
    }

    content = new ContactRelationResponseDTO(clientTwoData, true,
        (ContactRelationRequestDTO) content);
    clientTwo.sendResponse(content, packet);
    packet = clientOne.readPacket();

    if (packet == null) {
      Assertions.fail("No response for ContactRelationResponseDTO received.");
    }
    Assertions.assertInstanceOf(ContactRelationResponseDTO.class, content);
    LOGGER.info("Test: add contact -> success -- terminated");
  }

  @Test
  void removeContactSuccess() throws IOException {
    LOGGER.info("Test: remove contact -> success");
    Packet packet;
    PacketContent content;
    final ClientConnection clientOne, clientTwo;
    final CommunicatorDTO clientOneData, clientTwoData;

    clientOne = super.loginNextClient();

    super.addConnectionNextServer();
    clientTwo = super.getUnusedClientConnection();
    clientTwo.setClientData(TestClientDataProvider.PETER_1_AUTH, null);
    Assertions.assertTrue(clientTwo.tryClientLogin(), "Login failed.");

    clientOneData = clientOne.getCommunicatorData();
    clientTwoData = clientTwo.getCommunicatorData();

    content = new ContactRelationRequestDTO(EligibleContactEntity.CLIENT,
        clientOneData.getCommunicatorId(),
        PETER_1_COMM.getCommunicatorId(), clientOneData, false);
    clientOne.sendRequest(content, getClientEntity(PETER_1_COMM.getCommunicatorId()));
    packet = clientTwo.readPacket();

    if (packet != null) {
      content = packet.getPacketContent();
      Assertions.assertEquals(ContactRelationRequestDTO.class, content.getClass());
    } else {
      Assertions.fail("No response for ContactRelationRequestDTO received.");
    }
    packet = clientOne.readPacket();

    if (packet != null) {
      Assertions.assertInstanceOf(ContactRelationResponseDTO.class, packet.getPacketContent());
    } else {
      Assertions.fail("No response ContactRelationResponseDTO received.");
    }
    LOGGER.info("Test: remove contact -> success -- terminated");
  }

  @Test
  void contactRelationAddMissingFail() {
    LOGGER.info(
        "Test: add contact -> failure: invalid contact data");
    PacketContent content;
    final var clientOne = super.loginNextClient();
    final var clientData = clientOne.getCommunicatorData();

    content = new ContactRelationRequestDTO(EligibleContactEntity.CLIENT,
        clientData.getCommunicatorId(),
        STANDARD_SERVER_ID, null, true);
    TestResponseSingleClient.checkErrorResponse(clientOne, getClientEntity(STANDARD_SERVER_ID),
        content,
        "No communicator data specified.");
    LOGGER.info("Test: add contact -> failure: invalid contact data -- terminated");
  }

  @Test
  void addContactOfflineFail() {
    LOGGER.info("Test: add contact -> failure: contact offline");
    PacketContent content;
    final var clientOne = super.loginNextClient();
    final var clientData = clientOne.getCommunicatorData();

    content = new ContactRelationRequestDTO(EligibleContactEntity.CLIENT,
        clientData.getCommunicatorId(),
        ADRIAN_1_COMM.getCommunicatorId(), clientData, true);

    TestResponseSingleClient.checkErrorResponse(clientOne,
        getClientEntity(ADRIAN_1_COMM.getCommunicatorId()),
        content,
        "Packet could not be delivered. Contact offline.");
    LOGGER.info("Test: add contact -> failure: contact offline -- terminated");
  }

  @Test
  void addContactAlreadyFriendsFail() {
    LOGGER.info("Test: add contact -> failure: friends already");
    PacketContent content;
    final var clientOne = super.loginNextClient();
    final var clientData = clientOne.getCommunicatorData();

    content = new ContactRelationRequestDTO(EligibleContactEntity.CLIENT,
        clientData.getCommunicatorId(),
        MARKUS_1_COMM.getCommunicatorId(), clientData, true);
    TestResponseSingleClient.checkErrorResponse(clientOne,
        getClientEntity(MARKUS_1_COMM.getCommunicatorId()),
        content, "Friendship request was not processed. You already are friends with");
    LOGGER.info("Test: add contact -> failure: friends already -- terminated");
  }

  @Test
  void removeContactNoContactFail() {
    LOGGER.info("Test: remove contact -> failure: not a contact");
    PacketContent content;
    final var clientOne = super.loginNextClient();
    final var clientData = clientOne.getCommunicatorData();

    content = new ContactRelationRequestDTO(EligibleContactEntity.CLIENT,
        clientData.getCommunicatorId(),
        MAX_1_COMM.getCommunicatorId(), clientData, false);
    TestResponseSingleClient.checkErrorResponse(clientOne,
        getClientEntity(MAX_1_COMM.getCommunicatorId()), content,
        "is no contact of yours.");
    LOGGER.info("Test: remove contact -> failure: not a contact -- terminated");
  }
}
