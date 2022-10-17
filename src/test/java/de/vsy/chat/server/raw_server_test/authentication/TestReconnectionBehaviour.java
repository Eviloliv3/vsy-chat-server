/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package de.vsy.chat.server.raw_server_test.authentication;

import static de.vsy.shared_transmission.shared_transmission.packet.property.communicator.CommunicationEndpoint.getServerEntity;
import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_SERVER_ID;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.vsy.chat.server.raw_server_test.ServerPortProvider;
import de.vsy.chat.server.raw_server_test.ServerTestBase;
import de.vsy.chat.server.raw_server_test.TestClientDataProvider;
import de.vsy.chat.server.server_test_helpers.ClientConnection;
import de.vsy.chat.server.server_test_helpers.TestResponseSingleClient;
import de.vsy.shared_transmission.shared_transmission.dto.CommunicatorDTO;
import de.vsy.shared_transmission.shared_transmission.dto.authentication.AuthenticationDTO;
import de.vsy.shared_transmission.shared_transmission.packet.content.PacketContent;
import de.vsy.shared_transmission.shared_transmission.packet.content.authentication.ReconnectRequestDTO;
import de.vsy.shared_transmission.shared_transmission.packet.content.authentication.ReconnectResponseDTO;

/** @author fredward */
public class TestReconnectionBehaviour extends ServerTestBase {

	public TestReconnectionBehaviour(final ServerPortProvider clientConnectionPorts,
			final List<AuthenticationDTO> clientAuthenticationDataList) {
		super(clientConnectionPorts, clientAuthenticationDataList);
	}

	@Test
	void reconnectionFailAlreadyLoggedIn() {
		LOGGER.info("Test: Wiederverbindung fehlgeschlagen -> bereits eingeloggt");
		PacketContent content;
		final var clientOne = super.loginNextClient();
		final var clientOneCommunicatorData = clientOne.getCommunicatorData();
		content = new ReconnectRequestDTO(clientOneCommunicatorData);
		TestResponseSingleClient.checkErrorResponse(clientOne, getServerEntity(STANDARD_SERVER_ID), content,
				"Anfrage nicht bearbeitet. Sie sind bereits authentifiziert.");
		LOGGER.info("Test: Wiederverbindung fehlgeschlagen -> bereits eingeloggt -- beendet");
	}

	@Test
	void reconnectionFailReconnectionUnderway() throws InterruptedException, IOException {
		LOGGER.info("Test: Wiederverbindung fehlgeschlagen -> Versuch wird bereits unternommen");
		ReconnectRequestDTO request;
		final ClientConnection clientOne, clientTwo;
		final CommunicatorDTO clientOneCommunicatorData;

		clientOne = super.loginNextClient();
		clientOneCommunicatorData = clientOne.getCommunicatorData();

		super.addConnectionNextServer();
		clientTwo = super.getUnusedClientConnection();

		clientOne.resetConnection();

		request = new ReconnectRequestDTO(clientOneCommunicatorData);
		clientTwo.sendRequest(request, getServerEntity(STANDARD_SERVER_ID));

		TestResponseSingleClient.checkErrorResponse(clientOne, getServerEntity(STANDARD_SERVER_ID), request,
				"Sie sind entweder von einem anderen Gerät aus verbunden oder es wird bereits ein Wiederverbindungsversuch von einem anderen Gerät aus unternommen.");
		reconnectPendingClient(clientOne, request);

		LOGGER.info("Test: Wiederverbindung fehlgeschlagen -> Versuch wird bereits unternommen -- beendet");
	}

	@Test
	void reconnectionFailStillLoggedIn() throws IOException {
		LOGGER.info("Test: Wiederverbindung fehlgeschlagen -> noch von anderem Gerät eingeloggt");
		PacketContent content;
		final ClientConnection clientOne, clientTwo;
		final CommunicatorDTO clientOneCommunicatorData;

		clientOne = super.loginNextClient();
		clientOneCommunicatorData = clientOne.getCommunicatorData();

		super.addConnectionNextServer();
		clientTwo = super.getUnusedClientConnection();

		content = new ReconnectRequestDTO(clientOneCommunicatorData);
		TestResponseSingleClient.checkErrorResponse(clientTwo, getServerEntity(STANDARD_SERVER_ID), content,
				"Sie sind entweder von einem anderen Gerät aus verbunden oder es wird bereits ein Wiederverbindungsversuch von einem anderen Gerät aus unternommen.");
		LOGGER.info("Test: Wiederverbindung fehlgeschlagen -> noch von anderem Gerät eingeloggt -- beendet");
	}

	@Test
	void reconnectionFailNotAuthenticated() {
		LOGGER.info("Test: Wiederverbindung fehlgeschlagen -> nicht im schwebenden Zustand");
		PacketContent content;
		final var clientOne = super.getUnusedClientConnection();

		content = new ReconnectRequestDTO(TestClientDataProvider.FRANK_1_COMM);
		TestResponseSingleClient.checkErrorResponse(clientOne, getServerEntity(STANDARD_SERVER_ID), content,
				"Sie sind nicht als authentifiziert registriert.");
		LOGGER.info("Test: Wiederverbindung fehlgeschlagen -> nicht im schwebenden Zustand -- beendet");
	}

	@Test
	void reconnectionFailMalformedData() {
		LOGGER.info("Test: Wiederverbindung fehlgeschlagen -> ungültige Daten");
		PacketContent content;
		final var clientOne = super.getUnusedClientConnection();

		content = new ReconnectRequestDTO(CommunicatorDTO.valueOf(-4567, "Frank% Franke"));
		TestResponseSingleClient.checkErrorResponse(clientOne, getServerEntity(STANDARD_SERVER_ID), content,
				"Fehlerhafte Kommunikatordaten:");
		LOGGER.info("Test: Wiederverbindung fehlgeschlagen -> ungültige Daten -- beendet");
	}

	@Test
	void reconnectionFailFalseData() {
		LOGGER.info("Test: Wiederverbindung fehlgeschlagen -> fehlerhafte Daten");
		ReconnectRequestDTO erroneousRequest;
		final var clientOne = super.getUnusedClientConnection();

		erroneousRequest = new ReconnectRequestDTO(CommunicatorDTO.valueOf(123560, "Frank Falsch"));
		TestResponseSingleClient.checkErrorResponse(clientOne, getServerEntity(STANDARD_SERVER_ID), erroneousRequest,
				"Es existiert kein Account mit den von Ihnen angegebenen Daten.");
		LOGGER.info("Test: Wiederverbindung fehlgeschlagen -> fehlerhafte Daten -- beendet");
	}

	@Test
	void reconnectionSuccess() throws InterruptedException, IOException {
		LOGGER.info("Test: Wiederverbindung erfolgreich");
		ReconnectRequestDTO content;
		final ClientConnection clientOne, clientTwo;
		final CommunicatorDTO clientOneCommunicatorData;

		clientOne = super.loginNextClient();
		super.addConnectionNextServer();
		clientTwo = super.getUnusedClientConnection();
		clientOneCommunicatorData = clientOne.getCommunicatorData();

		clientOne.resetConnection();
		Thread.sleep(500);

		content = new ReconnectRequestDTO(clientOneCommunicatorData);
		reconnectPendingClient(clientTwo, content);
		LOGGER.info("Test: Wiederverbindung erfolgreich -- beendet");
	}

	private void reconnectPendingClient(ClientConnection connection, ReconnectRequestDTO request) {
		connection.setClientData(null, request.getClientData());
		TestResponseSingleClient.checkResponse(connection, getServerEntity(STANDARD_SERVER_ID), request,
				ReconnectResponseDTO.class);
	}
}
