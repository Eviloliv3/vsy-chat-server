package de.vsy.chat.server.raw_server_test.status;

import static de.vsy.chat.server.server_test_helpers.TestResponseSingleClient.checkErrorResponse;
import static de.vsy.chat.server.server_test_helpers.TestResponseSingleClient.checkResponse;
import static de.vsy.shared_transmission.packet.content.status.ClientService.MESSENGER;
import static de.vsy.shared_transmission.packet.property.communicator.CommunicationEndpoint.getServerEntity;
import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_SERVER_ID;

import de.vsy.chat.server.raw_server_test.ServerPortProvider;
import de.vsy.chat.server.raw_server_test.ServerTestBase;
import de.vsy.chat.server.server_test_helpers.ClientConnection;
import de.vsy.shared_transmission.dto.authentication.AuthenticationDTO;
import de.vsy.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.packet.content.PacketContent;
import de.vsy.shared_transmission.packet.content.status.ClientStatusChangeDTO;
import de.vsy.shared_transmission.packet.content.status.ContactMessengerStatusDTO;
import de.vsy.shared_transmission.packet.content.status.MessengerSetupDTO;
import de.vsy.shared_transmission.packet.content.status.MessengerTearDownDTO;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestStatusChange extends ServerTestBase {

  public TestStatusChange(final ServerPortProvider clientConnectionPorts,
      final List<AuthenticationDTO> clientAuthenticationDataList) {
    super(clientConnectionPorts, clientAuthenticationDataList);
  }

  @Test
  public void changeFromMessengerStateTwoClientSuccess() throws IOException {
    LOGGER.info("Test: Status zurücknehmen für mehrere Klienten -> erfolgreich -- gestartet");
    Packet packet;
    PacketContent content;
    final ClientConnection clientOne, clientTwo;

    super.addConnectionNextServer();
    clientOne = super.loginNextClient();
    clientTwo = super.loginNextClient();

    content = new ClientStatusChangeDTO(MESSENGER, true, clientOne.getCommunicatorData());
    checkResponse(clientOne, getServerEntity(STANDARD_SERVER_ID), content, MessengerSetupDTO.class);
    LOGGER.info("MessengerSetup 1 gelesen");
    content = new ClientStatusChangeDTO(MESSENGER, true, clientTwo.getCommunicatorData());
    checkResponse(clientTwo, getServerEntity(STANDARD_SERVER_ID), content, MessengerSetupDTO.class);
    LOGGER.info("MessengerSetup 2 gelesen");

    packet = clientOne.readPacket();
    LOGGER.info("Erwarte Kontaktstatusbenachrichtigung. Gelesen: {}", packet);

    Assertions.assertTrue(clientOne.tryClientLogout(), "Logout fehlgeschlagen.");
    packet = clientTwo.readPacket();

    LOGGER.info("erwarte Kontaktstatusbenachrichtigung. Gelesen: {}", packet);

    if (packet != null
        && packet.getPacketContent() instanceof ContactMessengerStatusDTO contactStatus) {
      Assertions.assertEquals(
          contactStatus.getContactData().equals(clientOne.getCommunicatorData()),
          !contactStatus.getOnlineStatus());
    } else {
      Assertions.fail(
          ContactMessengerStatusDTO.class.getSimpleName() + " erwartet, erhalten: " + packet);
    }
    clientOne.resetConnection();
    LOGGER.info("Test: Status zurücknehmen für mehrere Klienten gesetzt -> erfolgreich -- beendet");
  }

  @Test
  public void changeFromMessengerStateSingleClientSuccess() {
    LOGGER.info("Test: Status für einzelnen Klienten aufgehoben -> erfolgreich -- gestartet");
    Packet packet;
    PacketContent content;
    ClientConnection clientOne;
    clientOne = super.loginNextClient();

    content = new ClientStatusChangeDTO(MESSENGER, true, clientOne.getCommunicatorData());
    clientOne.sendRequest(content, getServerEntity(STANDARD_SERVER_ID));
    packet = clientOne.readPacket();

    if (packet != null) {
      content = new ClientStatusChangeDTO(MESSENGER, false, clientOne.getCommunicatorData());
      clientOne.sendRequest(content, getServerEntity(STANDARD_SERVER_ID));
      packet = clientOne.readPacket();

      if (packet != null) {
        content = packet.getPacketContent();
      } else {
        Assertions.fail("Keine Antwort MessengerTearDownDTO empfangen.");
      }
    } else {
      Assertions.fail("Keine Antwort MessengerSetupDTO empfangen.");
    }
    Assertions.assertInstanceOf(MessengerTearDownDTO.class, content);
    LOGGER.info("Test: Status für einzelnen Klienten aufgehoben -> erfolgreich -- beendet");
  }

  @Test
  public void changeToMessengerStateSingleClientSuccess() {
    LOGGER.info("Test: Status für einzelnen Klienten gesetzt -> erfolgreich -- gestartet");
    PacketContent content;
    final var clientOne = super.loginNextClient();

    content = new ClientStatusChangeDTO(MESSENGER, true, clientOne.getCommunicatorData());
    checkResponse(clientOne, getServerEntity(STANDARD_SERVER_ID), content, MessengerSetupDTO.class);
    LOGGER.info("Test: Status für einzelnen Klienten gesetzt -> erfolgreich -- beendet");
  }

  @Test
  public void changeMessengerNoStateFailed() {
    LOGGER.info("Test: Status nicht erreichbar -> kein Status angegeben -- gestartet");
    ClientStatusChangeDTO content;
    final var clientOne = super.loginNextClient();

    content = new ClientStatusChangeDTO(null, true, clientOne.getCommunicatorData());
    checkErrorResponse(clientOne, getServerEntity(STANDARD_SERVER_ID), content,
        "Ungültige Klientenstatusmitteilung. Kein Servicetyp angegeben.");
    LOGGER.info("Test: Status nicht erreichbar -> kein Status angegeben -- beendet");
  }

  @Test
  public void changeToMessengerStateTwoClientSuccess() throws IOException {
    LOGGER.info("Test: Status für mehrere Klienten gesetzt -> erfolgreich -- gestartet");
    Packet packet;
    PacketContent content;
    final ClientConnection clientOne, clientTwo;

    super.addConnectionNextServer();
    clientOne = super.loginNextClient();
    clientTwo = super.loginNextClient();

    content = new ClientStatusChangeDTO(MESSENGER, true, clientOne.getCommunicatorData());
    checkResponse(clientOne, getServerEntity(STANDARD_SERVER_ID), content, MessengerSetupDTO.class);
    LOGGER.info("MessengerSetup 1 gelesen");
    content = new ClientStatusChangeDTO(MESSENGER, true, clientTwo.getCommunicatorData());
    checkResponse(clientTwo, getServerEntity(STANDARD_SERVER_ID), content, MessengerSetupDTO.class);
    LOGGER.info("MessengerSetup 2 gelesen");

    packet = clientOne.readPacket();
    LOGGER.info("Erwarte Kontaktstatusbenachrichtigung. Gelesen: {}", packet);

    if (packet != null
        && packet.getPacketContent() instanceof ContactMessengerStatusDTO contactStatus) {
      Assertions.assertEquals(contactStatus.getContactData(), clientTwo.getCommunicatorData());
    } else {
      Assertions.fail(
          ContactMessengerStatusDTO.class.getSimpleName() + " erwartet, erhalten: " + packet);
    }
    LOGGER.info("Test: Status für mehrere Klienten gesetzt -> erfolgreich -- beendet");
  }
}
