package de.vsy.chat.server.raw_server_test.authentication;

import static de.vsy.chat.server.raw_server_test.TestClientDataProvider.FRANK_1_COMM;
import static de.vsy.shared_transmission.packet.property.communicator.CommunicationEndpoint.getServerEntity;
import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_SERVER_ID;
import static de.vsy.shared_utility.standard_value.StandardStringProvider.STANDARD_EMPTY_STRING;

import de.vsy.chat.server.raw_server_test.ServerPortProvider;
import de.vsy.chat.server.raw_server_test.ServerTestBase;
import de.vsy.chat.server.raw_server_test.TestClientDataProvider;
import de.vsy.chat.server.server_test_helpers.TestResponseSingleClient;
import de.vsy.shared_transmission.dto.authentication.AuthenticationDTO;
import de.vsy.shared_transmission.dto.authentication.PersonalData;
import de.vsy.shared_transmission.dto.builder.AccountCreationDTOBuilder;
import de.vsy.shared_transmission.packet.content.PacketContent;
import de.vsy.shared_transmission.packet.content.authentication.LoginResponseDTO;
import de.vsy.shared_transmission.packet.content.authentication.NewAccountRequestDTO;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class TestAccountCreationBehaviour extends ServerTestBase {

  public TestAccountCreationBehaviour(final ServerPortProvider clientConnectionPorts,
      final List<AuthenticationDTO> clientAuthenticationDataList) {
    super(clientConnectionPorts, clientAuthenticationDataList);
  }

  @Test
  void newAccountSuccess() {
    LOGGER.info("Test: create account -> success");
    PacketContent content;
    final var clientOne = super.getUnusedClientConnection();
    var accountCreationBuilder = new AccountCreationDTOBuilder();
    final var randomAppendix = ThreadLocalRandom.current().nextInt(10000);

    accountCreationBuilder.withAuthenticationData(
            AuthenticationDTO.valueOf("peter" + randomAppendix, "login"))
        .withPersonalData(PersonalData.valueOf("randomPeter" + randomAppendix, "random"));
    content = new NewAccountRequestDTO(accountCreationBuilder.build());
    TestResponseSingleClient.checkResponse(clientOne, getServerEntity(STANDARD_SERVER_ID), content,
        LoginResponseDTO.class);
    LOGGER.info("Test: create account -> success -- terminated");
  }

  @Test
  void newAccountFailMalformedData() {
    LOGGER.info("Test: create account -> failure: invalid data");
    PacketContent content;
    final var clientOne = super.getUnusedClientConnection();
    var accountCreationBuilder = new AccountCreationDTOBuilder();

    accountCreationBuilder.withAuthenticationData(
            AuthenticationDTO.valueOf("123456", STANDARD_EMPTY_STRING))
        .withPersonalData(PersonalData.valueOf("34fsjö5&", "jsdfj34ßtm"));
    content = new NewAccountRequestDTO(accountCreationBuilder.build());
    TestResponseSingleClient.checkErrorResponse(clientOne, getServerEntity(STANDARD_SERVER_ID),
        content,
        "Invalid credentials:");
    LOGGER.info("Test: create account -> failure: invalid data -- terminated");
  }

  @Test
  void newAccountExistsFail() {
    LOGGER.info("Test: create account -> failure: account already exists");
    PacketContent content;
    final var clientOne = super.getUnusedClientConnection();
    var accountCreationBuilder = new AccountCreationDTOBuilder();

    accountCreationBuilder.withAuthenticationData(TestClientDataProvider.FRANK_1_AUTH)
        .withPersonalData(PersonalData.valueOf("Frank", "Relation1"));
    content = new NewAccountRequestDTO(accountCreationBuilder.build());
    TestResponseSingleClient.checkErrorResponse(clientOne, getServerEntity(STANDARD_SERVER_ID),
        content,
        "No account was created. There is an existing account with the provided login data.");
    LOGGER.info("Test: create account -> failure: account already exists -- terminated");
  }

  @Test
  void newAccountAlreadyAuthenticatedFail() {
    LOGGER.info("Test: create account -> failure: already logged in");
    PacketContent content;
    var client = loginNextClient();
    var accountCreationBuilder = new AccountCreationDTOBuilder();

    var labelComponents = FRANK_1_COMM.getDisplayLabel().split(" ");
    accountCreationBuilder.withAuthenticationData(TestClientDataProvider.FRANK_1_AUTH)
        .withPersonalData(PersonalData.valueOf(labelComponents[0], labelComponents[1]));
    content = new NewAccountRequestDTO(accountCreationBuilder.build());
    TestResponseSingleClient.checkErrorResponse(client, getServerEntity(STANDARD_SERVER_ID),
        content,
        "Request not processed. You are authenticated already.");
    LOGGER.info("Test: create account -> failure: already logged in -- terminated");
  }
}
