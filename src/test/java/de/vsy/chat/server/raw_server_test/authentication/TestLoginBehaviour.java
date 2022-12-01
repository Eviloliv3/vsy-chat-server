package de.vsy.chat.server.raw_server_test.authentication;

import static de.vsy.shared_transmission.packet.property.communicator.CommunicationEndpoint.getServerEntity;
import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_SERVER_ID;
import static de.vsy.shared_utility.standard_value.StandardStringProvider.STANDARD_EMPTY_STRING;

import de.vsy.chat.server.raw_server_test.ServerPortProvider;
import de.vsy.chat.server.raw_server_test.ServerTestBase;
import de.vsy.chat.server.raw_server_test.TestClientDataProvider;
import de.vsy.chat.server.server_test_helpers.ClientConnection;
import de.vsy.chat.server.server_test_helpers.TestResponseSingleClient;
import de.vsy.shared_transmission.dto.authentication.AuthenticationDTO;
import de.vsy.shared_transmission.packet.content.PacketContent;
import de.vsy.shared_transmission.packet.content.authentication.LoginRequestDTO;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 
 */
public class TestLoginBehaviour extends ServerTestBase {

  public TestLoginBehaviour(final ServerPortProvider clientConnectionPorts,
      final List<AuthenticationDTO> clientAuthenticationDataList) {
    super(clientConnectionPorts, clientAuthenticationDataList);
  }

  @Test
  void loginFailLoggedInFromDifferentClient() throws IOException {
    LOGGER.info("Test: login -> failure: already logged in from another device");
    PacketContent content;
    ClientConnection clientOne, clientTwo;
    AuthenticationDTO clientOneAuth;

    clientOne = super.loginNextClient();
    clientOneAuth = clientOne.getAuthenticationData();

    super.addConnectionNextServer();
    clientTwo = super.getUnusedClientConnection();

    content = new LoginRequestDTO(clientOneAuth.getUsername(), clientOneAuth.getPassword());

    TestResponseSingleClient.checkErrorResponse(clientTwo, getServerEntity(STANDARD_SERVER_ID),
        content,
        "You already are connected from another device.");
    LOGGER.info("Test: login -> failure: already logged in from another device -- terminated");
  }

  @Test
  void loginFailNoLogin() {
    LOGGER.info("Test: login -> failure: erroneous credentials");
    PacketContent content;
    final var clientOne = super.getUnusedClientConnection();

    content = new LoginRequestDTO(STANDARD_EMPTY_STRING, STANDARD_EMPTY_STRING);
    TestResponseSingleClient.checkErrorResponse(clientOne, getServerEntity(STANDARD_SERVER_ID),
        content,
        "Invalid credentials:");
    LOGGER.info("Test: login -> failure: erroneous credentials -- terminated");
  }

  @Test
  void loginFailFalseCredentials() {
    LOGGER.info("Test: login -> failure: no account");
    PacketContent content;
    final var clientOne = super.getUnusedClientConnection();

    content = new LoginRequestDTO("frank1", "wrong");
    TestResponseSingleClient.checkErrorResponse(clientOne, getServerEntity(STANDARD_SERVER_ID),
        content,
        "No account data found for your credentials.");
    LOGGER.info("Test: login -> failure: no account -- terminated");
  }

  @Test
  void loginFailAlreadyLoggedIn() {
    LOGGER.info("Test: login -> failure: already authenticated on this connection");
    ClientConnection clientOne;
    AuthenticationDTO clientOneAuthenticationData;
    PacketContent content;

    clientOne = super.loginNextClient();
    clientOneAuthenticationData = clientOne.getAuthenticationData();
    content = new LoginRequestDTO(clientOneAuthenticationData.getUsername(),
        clientOneAuthenticationData.getPassword());

    TestResponseSingleClient.checkErrorResponse(clientOne, getServerEntity(STANDARD_SERVER_ID),
        content,
        "Request not processed. You are authenticated already.");
    LOGGER.info("Test: login -> failure: already authenticated on this connection -- terminated");
  }

  @Test
  void loginSuccess() {
    LOGGER.info("Test: login -> success");
    boolean loginSuccess;
    final var clientOne = super.getUnusedClientConnection();
    clientOne.setClientData(TestClientDataProvider.FRANK_1_AUTH, null);
    Assertions.assertTrue(clientOne.tryClientLogin(), "Login failed.");
    LOGGER.info("Test: login -> success -- terminated");
  }
}
