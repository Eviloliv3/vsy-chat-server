package de.vsy.chat.server.raw_server_test.chat;

import static de.vsy.chat.server.raw_server_test.TestClientDataProvider.FRANK_1_AUTH;
import static de.vsy.chat.server.raw_server_test.TestClientDataProvider.MARKUS_1_AUTH;
import static de.vsy.chat.server.raw_server_test.TestClientDataProvider.THOMAS_1_AUTH;
import static de.vsy.chat.server.server_test_helpers.TestResponseSingleClient.checkErrorResponse;
import static de.vsy.chat.server.server_test_helpers.TestResponseSingleClient.checkResponse;
import static de.vsy.shared_transmission.shared_transmission.packet.content.status.ClientService.MESSENGER;
import static de.vsy.shared_transmission.shared_transmission.packet.property.communicator.CommunicationEndpoint.getClientEntity;
import static de.vsy.shared_transmission.shared_transmission.packet.property.communicator.CommunicationEndpoint.getServerEntity;
import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_SERVER_ID;

import de.vsy.chat.server.raw_server_test.ServerPortProvider;
import de.vsy.chat.server.raw_server_test.ServerTestBase;
import de.vsy.chat.server.server_test_helpers.ClientConnection;
import de.vsy.shared_transmission.shared_transmission.dto.authentication.AuthenticationDTO;
import de.vsy.shared_transmission.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.shared_transmission.packet.content.PacketContent;
import de.vsy.shared_transmission.shared_transmission.packet.content.chat.TextMessageDTO;
import de.vsy.shared_transmission.shared_transmission.packet.content.relation.EligibleContactEntity;
import de.vsy.shared_transmission.shared_transmission.packet.content.status.ClientStatusChangeDTO;
import de.vsy.shared_transmission.shared_transmission.packet.content.status.MessengerSetupDTO;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestClientMessage extends ServerTestBase {

  public TestClientMessage(final ServerPortProvider clientConnectionPorts,
      final List<AuthenticationDTO> clientAuthenticationDataList) {
    super(clientConnectionPorts, clientAuthenticationDataList);
  }

  @Test
  void sendMessageContactNotActiveFail() throws IOException {
    LOGGER.info("Test: Nachricht Senden Fehlschlag --> Kontakt offline -- gestartet");
    final ClientConnection clientOne, clientTwo;

    clientOne = super.loginNextClient();
    super.addConnectionNextServer();
    clientTwo = super.loginNextClient();

    changeStatus(clientOne, true);

    final var inactiveClientId = clientTwo.getCommunicatorData().getCommunicatorId();

    final var message = new TextMessageDTO(clientOne.getCommunicatorData().getCommunicatorId(),
        EligibleContactEntity.CLIENT, inactiveClientId, "Testnachricht.");

    checkErrorResponse(clientOne, getClientEntity(inactiveClientId), message,
        "Das Paket wurde nicht zugestellt. Paket wurde nicht zugestellt. Kontakt offline.");
    LOGGER.info("Test: Nachricht Senden Fehlschlag --> Kontakt offline -- beendet");
  }

  private void changeStatus(ClientConnection connection, boolean changeTo) {
    PacketContent content = new ClientStatusChangeDTO(MESSENGER, changeTo,
        connection.getCommunicatorData());
    checkResponse(connection, getServerEntity(STANDARD_SERVER_ID), content,
        MessengerSetupDTO.class);
  }

  @Test
  void sendMessageNoContactFail() throws IOException {
    LOGGER.info("Test: Nachricht Senden Fehlschlag --> kein Kontakt -- gestartet");
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
        EligibleContactEntity.CLIENT, noContactId, "Testnachricht.");

    checkErrorResponse(clientOne, getClientEntity(noContactId), message,
        "Textnachricht wurde nicht zugestellt. 15006 ist kein Kontakt von Ihnen.");
    LOGGER.info("Test: Nachricht Senden Fehlschlag --> kein Kontakt -- beendet");
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
      Assertions.fail("Keine freie Verbindung gefunden.");
    }
    return null;
  }

  @Test
  void sendMessageSuccess() throws IOException {
    LOGGER.info("Test: Nachricht Senden erfolgreich -- gestartet");
    Packet sentPacket, receivedPacket, responsePacket;
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
        EligibleContactEntity.CLIENT, contactId, "Testnachricht.");

    sentPacket = clientOne.sendRequest(message, getClientEntity(contactId));
    receivedPacket = clientTwo.readPacket();
    responsePacket = clientOne.readPacket();

    if (sentPacket != null && receivedPacket != null && responsePacket != null) {
      if (sentPacket.getPacketContent() instanceof TextMessageDTO textMessage
          && receivedPacket.getPacketContent() instanceof TextMessageDTO receivedMessage
          && responsePacket.getPacketContent() instanceof TextMessageDTO responseMessage) {
        Assertions.assertEquals(textMessage.getMessage(), receivedMessage.getMessage(),
            responseMessage.getMessage());
      } else {
        Assertions.fail(
            "Fehlerhafter Paketinhalt:\nGesandt: " + sentPacket.getPacketContent() + "\nEmpfangen: "
                + receivedPacket.getPacketContent() + "\nAntwort: "
                + responsePacket.getPacketContent());
      }
    } else {
      Assertions.fail("Eines der erwarteten Pakete fehlt:\nGesandt: " + sentPacket + "\nEmpfangen: "
          + receivedPacket + "\nAntwort: " + responsePacket);
    }
    LOGGER.info("Test: Nachricht Senden erfolgreich -- beendet");
  }
}
