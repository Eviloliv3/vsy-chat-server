package de.vsy.chat.server.raw_server_test.authentication;

import de.vsy.chat.server.raw_server_test.ServerPortProvider;
import de.vsy.chat.server.raw_server_test.ServerTestBase;
import de.vsy.chat.server.raw_server_test.TestClientDataProvider;
import de.vsy.chat.server.server_test_helpers.TestResponseSingleClient;
import de.vsy.shared_transmission.shared_transmission.dto.authentication.AuthenticationDTO;
import de.vsy.shared_transmission.shared_transmission.dto.authentication.PersonalData;
import de.vsy.shared_transmission.shared_transmission.dto.builder.AccountCreationDTOBuilder;
import de.vsy.shared_transmission.shared_transmission.packet.content.PacketContent;
import de.vsy.shared_transmission.shared_transmission.packet.content.authentication.LoginResponseDTO;
import de.vsy.shared_transmission.shared_transmission.packet.content.authentication.NewAccountRequestDTO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static de.vsy.shared_transmission.shared_transmission.dto.authentication.PersonalData.valueOf;
import static de.vsy.shared_transmission.shared_transmission.packet.property.communicator.CommunicationEndpoint.getServerEntity;
import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_SERVER_ID;
import static de.vsy.shared_utility.standard_value.StandardStringProvider.STANDARD_EMPTY_STRING;

/** @author fredward */
public
class TestAccountCreationBehaviour extends ServerTestBase {

    public
    TestAccountCreationBehaviour (final ServerPortProvider clientConnectionPorts,
                                  final List<AuthenticationDTO> clientAuthenticationDataList) {
        super(clientConnectionPorts, clientAuthenticationDataList);
    }

    @Test
    void newAccountSuccess () {
        LOGGER.info("Test: Neues Konto erstellen -> Erfolg");
        PacketContent content;
        final var clientOne = super.getUnusedClientConnection();
        var accountCreationBuilder = new AccountCreationDTOBuilder();
        final var randomAppendix = ThreadLocalRandom.current().nextInt(10000);

        accountCreationBuilder.withAuthenticationData(
                                      AuthenticationDTO.valueOf("Peter" + randomAppendix, "login"))
                              .withPersonalData(PersonalData.valueOf(
                                      "ZufallsPeter" + randomAppendix, "Zufall"));
        content = new NewAccountRequestDTO(accountCreationBuilder.build());
        TestResponseSingleClient.checkResponse(clientOne,
                                               getServerEntity(STANDARD_SERVER_ID),
                                               content, LoginResponseDTO.class);
        LOGGER.info("Test: Neues Konto erstellen -> Erfolg -- beendet");
    }

    @Test
    void newAccountFailMalformedData () {
        LOGGER.info("Test: Neues Konto nicht erstellt -> ungültigeDaten");
        PacketContent content;
        final var clientOne = super.getUnusedClientConnection();
        var accountCreationBuilder = new AccountCreationDTOBuilder();

        accountCreationBuilder.withAuthenticationData(
                                      AuthenticationDTO.valueOf("123456", STANDARD_EMPTY_STRING))
                              .withPersonalData(PersonalData.valueOf("34fsjö5&",
                                                                     "jsdfj34ßtm"));
        content = new NewAccountRequestDTO(accountCreationBuilder.build());
        TestResponseSingleClient.checkErrorResponse(clientOne, getServerEntity(
                STANDARD_SERVER_ID), content, "Fehlerhafte Klientendaten:");
        LOGGER.info("Test: Neues Konto nicht erstellt -> ungültigeDaten -- beendet");
    }

    @Test
    void newAccountExistsFail () {
        LOGGER.info("Test: Neues Konto nicht erstellt -> Konto existiert bereits");
        PacketContent content;
        final var clientOne = super.getUnusedClientConnection();
        var accountCreationBuilder = new AccountCreationDTOBuilder();

        accountCreationBuilder.withAuthenticationData(
                                      TestClientDataProvider.FRANK_1_AUTH)
                              .withPersonalData(
                                      PersonalData.valueOf("Frank", "Relation1"));
        content = new NewAccountRequestDTO(accountCreationBuilder.build());
        TestResponseSingleClient.checkErrorResponse(clientOne, getServerEntity(
                                                            STANDARD_SERVER_ID), content,
                                                    "Es gibt bereits einen Account mit den, von Ihnen, eingegebenen Login-Daten.");
        LOGGER.info(
                "Test: Neues Konto nicht erstellt -> Konto existiert bereits -- beendet");
    }

    @Test
    void newAccountAlreadyAuthenticatedFail () {
        LOGGER.info("Test: Neues Konto nicht erstellt -> bereits eingeloggt");
        PacketContent content;
        var client = loginNextClient();
        var accountCreationBuilder = new AccountCreationDTOBuilder();

        accountCreationBuilder.withAuthenticationData(
                                      TestClientDataProvider.FRANK_1_AUTH)
                              .withPersonalData(valueOf("Frank", "Franke"));
        content = new NewAccountRequestDTO(accountCreationBuilder.build());
        TestResponseSingleClient.checkErrorResponse(client, getServerEntity(
                                                            STANDARD_SERVER_ID), content,
                                                    "Anfrage nicht bearbeitet. Sie sind bereits authentifiziert.");
        LOGGER.info(
                "Test: Neues Konto nicht erstellt -> bereits eingeloggt -- beendet");
    }
}
