package de.vsy.chat.server.raw_server_test.authentication;

import de.vsy.chat.server.raw_server_test.ServerPortProvider;
import de.vsy.chat.server.raw_server_test.ServerTestBase;
import de.vsy.chat.server.server_test_helpers.ClientConnection;
import de.vsy.chat.shared_transmission.dto.authentication.AuthenticationDTO;
import de.vsy.chat.shared_transmission.packet.content.PacketContent;
import de.vsy.chat.shared_transmission.packet.content.authentication.LoginRequestDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static de.vsy.chat.server.raw_server_test.TestClientDataProvider.FRANK_1_AUTH;
import static de.vsy.chat.server.server_test_helpers.TestResponseSingleClient.checkErrorResponse;
import static de.vsy.chat.shared_transmission.packet.property.communicator.CommunicationEndpoint.getServerEntity;
import static de.vsy.chat.shared_utility.standard_value.StandardIdProvider.STANDARD_SERVER_ID;
import static de.vsy.chat.shared_utility.standard_value.StandardStringProvider.STANDARD_EMPTY_STRING;

/** @author fredward */
public
class TestLoginBehaviour extends ServerTestBase {

    public
    TestLoginBehaviour (final ServerPortProvider clientConnectionPorts,
                        final List<AuthenticationDTO> clientAuthenticationDataList) {
        super(clientConnectionPorts, clientAuthenticationDataList);
    }

    @Test
    void loginFailLoggedInFromDifferentClient ()
    throws IOException {
        LOGGER.info(
                "Test: Login eingeloggt -> bereits von anderem Gerät aus eingeloggt.");
        PacketContent content;
        ClientConnection clientOne, clientTwo;
        AuthenticationDTO clientOneAuth;

        clientOne = super.loginNextClient();
        clientOneAuth = clientOne.getAuthenticationData();

        super.addConnectionSameServer();
        clientTwo = super.getUnusedClientConnection();

        content = new LoginRequestDTO(clientOneAuth.getLogin(),
                                      clientOneAuth.getPassword());

        checkErrorResponse(clientTwo, getServerEntity(STANDARD_SERVER_ID), content,
                           "Sie sind bereits von einem anderen Gerät aus angemeldet.");
        LOGGER.info(
                "Test: Login eingeloggt -> bereits von anderem Gerät aus eingeloggt. --beendet");
    }

    @Test
    void loginFailNoLogin () {
        LOGGER.info("Test: Login falsch -> fehlerhafte Daten");
        PacketContent content;
        final var clientOne = super.getUnusedClientConnection();

        content = new LoginRequestDTO(STANDARD_EMPTY_STRING, STANDARD_EMPTY_STRING);
        checkErrorResponse(clientOne, getServerEntity(STANDARD_SERVER_ID), content,
                           "Fehlerhafte Klientendaten:");
        LOGGER.info("Test: Login falsch -> fehlerhafte Daten -- beendet");
    }

    @Test
    void loginFailFalseCredentials () {
        LOGGER.info("Test: Login falsch -> Kein Konto");
        PacketContent content;
        final var clientOne = super.getUnusedClientConnection();

        content = new LoginRequestDTO("frank1", "falsch");
        checkErrorResponse(clientOne, getServerEntity(STANDARD_SERVER_ID), content,
                           "Es wurde kein Konto für die von Ihnen eingegebenen Login-Daten gefunden.");
        LOGGER.info("Test: Login falsch -> Kein Konto --beendet");
    }

    @Test
    void loginFailAlreadyLoggedIn () {
        LOGGER.info("Test: Login richtig -> schon eingeloggt");
        ClientConnection clientOne;
        AuthenticationDTO clientOneAuthenticationData;
        PacketContent content;

        clientOne = super.loginNextClient();
        clientOneAuthenticationData = clientOne.getAuthenticationData();
        content = new LoginRequestDTO(clientOneAuthenticationData.getLogin(),
                                      clientOneAuthenticationData.getPassword());

        checkErrorResponse(clientOne, getServerEntity(STANDARD_SERVER_ID), content,
                           "Anfrage nicht bearbeitet. Sie sind bereits authentifiziert.");
        LOGGER.info("Test: Login richtig -> schon eingeloggt --beendet");
    }

    @Test
    void loginSuccess () {
        LOGGER.info("Test: Login erfolgreich");
        boolean loginSuccess;
        final var clientOne = super.getUnusedClientConnection();
        clientOne.setClientData(FRANK_1_AUTH, null);

        loginSuccess = clientOne.tryClientLogin();
        Assertions.assertTrue((loginSuccess), "Login fehlgeschlagen.");
        LOGGER.info("Test: Login falsch -> fehlerhafte Daten -- beendet");
    }
}
