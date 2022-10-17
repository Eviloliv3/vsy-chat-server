/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package de.vsy.chat.server.raw_server_test.authentication;

import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_SERVER_ID;

import de.vsy.chat.server.raw_server_test.ServerPortProvider;
import de.vsy.chat.server.raw_server_test.ServerTestBase;
import de.vsy.chat.server.raw_server_test.TestClientDataProvider;
import de.vsy.chat.server.server_test_helpers.TestResponseSingleClient;
import de.vsy.shared_transmission.shared_transmission.dto.authentication.AuthenticationDTO;
import de.vsy.shared_transmission.shared_transmission.packet.content.authentication.LogoutRequestDTO;
import de.vsy.shared_transmission.shared_transmission.packet.property.communicator.CommunicationEndpoint;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author fredward
 */
public class TestLogoutBehaviour extends ServerTestBase {

  public TestLogoutBehaviour(final ServerPortProvider clientConnectionPorts,
      final List<AuthenticationDTO> clientAuthenticationDataList) {
    super(clientConnectionPorts, clientAuthenticationDataList);
  }

  @Test
  void logoutFailNoLogin() {
    LOGGER.info("Test: Logout fehlgeschlagen -> nicht eingeloggt");

    final var clientOne = super.getUnusedClientConnection();
    final var logoutContent = new LogoutRequestDTO(clientOne.getCommunicatorData());

    TestResponseSingleClient.checkErrorResponse(clientOne,
        CommunicationEndpoint.getServerEntity(STANDARD_SERVER_ID), logoutContent,
        "Anfrage nicht bearbeitet. Sie sind noch nicht authentifiziert.");
    LOGGER.info("Test: Logout fehlgeschlagen -> nicht eingeloggt -- beendet");
  }

  @Test
  void logoutSuccess() throws IOException {
    LOGGER.info("Test: Logout erfolgreich");
    boolean authSuccess;
    final var clientOne = super.getUnusedClientConnection();
    clientOne.setClientData(TestClientDataProvider.FRANK_1_AUTH, null);
    Assertions.assertTrue(clientOne.tryClientLogin(), "Login fehlgeschlagen.");
    Assertions.assertTrue(clientOne.tryClientLogout(), "Logout fehlgeschlagen.");
    clientOne.resetConnection();
    LOGGER.info("Test: Logout erfolgreich -- beendet");
  }
}
