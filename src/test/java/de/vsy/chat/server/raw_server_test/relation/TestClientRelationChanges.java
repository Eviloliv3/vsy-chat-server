package de.vsy.chat.server.raw_server_test.relation;

import static de.vsy.chat.server.raw_server_test.TestClientDataProvider.ADRIAN_1_COMM;
import static de.vsy.chat.server.raw_server_test.TestClientDataProvider.MARKUS_1_COMM;
import static de.vsy.chat.server.raw_server_test.TestClientDataProvider.MAX_1_COMM;
import static de.vsy.chat.server.raw_server_test.TestClientDataProvider.PETER_1_COMM;
import static de.vsy.shared_transmission.shared_transmission.packet.property.communicator.CommunicationEndpoint.getClientEntity;
import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_SERVER_ID;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.vsy.chat.server.raw_server_test.ServerPortProvider;
import de.vsy.chat.server.raw_server_test.ServerTestBase;
import de.vsy.chat.server.raw_server_test.TestClientDataProvider;
import de.vsy.chat.server.server_test_helpers.ClientConnection;
import de.vsy.chat.server.server_test_helpers.TestResponseSingleClient;
import de.vsy.shared_transmission.shared_transmission.dto.CommunicatorDTO;
import de.vsy.shared_transmission.shared_transmission.dto.authentication.AuthenticationDTO;
import de.vsy.shared_transmission.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.shared_transmission.packet.content.PacketContent;
import de.vsy.shared_transmission.shared_transmission.packet.content.relation.ContactRelationRequestDTO;
import de.vsy.shared_transmission.shared_transmission.packet.content.relation.ContactRelationResponseDTO;
import de.vsy.shared_transmission.shared_transmission.packet.content.relation.EligibleContactEntity;

public class TestClientRelationChanges extends ServerTestBase {

	public TestClientRelationChanges(final ServerPortProvider clientConnectionPorts,
			final List<AuthenticationDTO> clientAuthenticationDataList) {
		super(clientConnectionPorts, clientAuthenticationDataList);
	}

	@Test
	void addContactSuccess() throws IOException {
		Packet packet;
		PacketContent content;
		final ClientConnection clientOne, clientTwo;
		final CommunicatorDTO clientOneData, clientTwoData;

		clientOne = super.loginNextClient();

		super.addConnectionNextServer();
		clientTwo = super.getUnusedClientConnection();
		clientTwo.setClientData(TestClientDataProvider.ADRIAN_1_AUTH, null);
		clientTwo.tryClientLogin();

		clientOneData = clientOne.getCommunicatorData();
		clientTwoData = clientTwo.getCommunicatorData();

		content = new ContactRelationRequestDTO(EligibleContactEntity.CLIENT, clientOneData.getCommunicatorId(),
				ADRIAN_1_COMM.getCommunicatorId(), clientOneData, true);

		clientOne.sendRequest(content, getClientEntity(ADRIAN_1_COMM.getCommunicatorId()));
		packet = clientTwo.readPacket();

		if (packet == null || !(packet.getPacketContent() instanceof ContactRelationRequestDTO)) {
			Assertions.fail("Keine Antwort ContactRelationRequestDTO empfangen.");
		} else {
			content = packet.getPacketContent();
		}

		content = new ContactRelationResponseDTO(clientTwoData, true, (ContactRelationRequestDTO) content);
		clientTwo.sendResponse(content, packet);
		packet = clientOne.readPacket();

		if (packet == null) {
			Assertions.fail("Keine Antwort ContactRelationResponseDTO empfangen.");
		}
		Assertions.assertInstanceOf(ContactRelationResponseDTO.class, content);
	}

	@Test
	void removeContactSuccess() throws IOException {
		Packet packet;
		PacketContent content;
		final ClientConnection clientOne, clientTwo;
		final CommunicatorDTO clientOneData, clientTwoData;

		clientOne = super.loginNextClient();

		super.addConnectionNextServer();
		clientTwo = super.getUnusedClientConnection();
		clientTwo.setClientData(TestClientDataProvider.PETER_1_AUTH, null);
		clientTwo.tryClientLogin();

		clientOneData = clientOne.getCommunicatorData();
		clientTwoData = clientTwo.getCommunicatorData();

		content = new ContactRelationRequestDTO(EligibleContactEntity.CLIENT, clientOneData.getCommunicatorId(),
				PETER_1_COMM.getCommunicatorId(), clientOneData, false);
		clientOne.sendRequest(content, getClientEntity(PETER_1_COMM.getCommunicatorId()));
		packet = clientTwo.readPacket();

		if (packet != null) {
			content = packet.getPacketContent();
			Assertions.assertEquals(ContactRelationRequestDTO.class, content.getClass());
		} else {
			Assertions.fail("Keine Antwort ContactRelationRequestDTO empfangen.");
		}
		packet = clientOne.readPacket();

		if (packet != null) {
			Assertions.assertInstanceOf(ContactRelationResponseDTO.class, packet.getPacketContent());
		} else {
			Assertions.fail("Keine Antwort ContactRelationResponseDTO empfangen.");
		}
	}

	@Test
	void contactRelationAddMissingFail() {
		PacketContent content;
		final var clientOne = super.loginNextClient();
		final var clientData = clientOne.getCommunicatorData();

		content = new ContactRelationRequestDTO(EligibleContactEntity.CLIENT, clientData.getCommunicatorId(),
				STANDARD_SERVER_ID, null, true);
		TestResponseSingleClient.checkErrorResponse(clientOne, getClientEntity(STANDARD_SERVER_ID), content,
				"Ungültige Kontaktanfrage. Fehlerhafte Kommunikatordaten: Es sind keine Kommunikatordaten vorhanden.");
	}

	@Test
	void addContactOfflineFail() {
		PacketContent content;
		final var clientOne = super.loginNextClient();
		final var clientData = clientOne.getCommunicatorData();

		content = new ContactRelationRequestDTO(EligibleContactEntity.CLIENT, clientData.getCommunicatorId(),
				ADRIAN_1_COMM.getCommunicatorId(), clientData, true);

		TestResponseSingleClient.checkErrorResponse(clientOne, getClientEntity(ADRIAN_1_COMM.getCommunicatorId()),
				content, "Das Paket wurde nicht zugestellt. Paket wurde nicht zugestellt. Kontakt offline.");
	}

	@Test
	void addContactAlreadyFriendsFail() {
		PacketContent content;
		final var clientOne = super.loginNextClient();
		final var clientData = clientOne.getCommunicatorData();

		content = new ContactRelationRequestDTO(EligibleContactEntity.CLIENT, clientData.getCommunicatorId(),
				MARKUS_1_COMM.getCommunicatorId(), clientData, true);
		TestResponseSingleClient.checkErrorResponse(clientOne, getClientEntity(MARKUS_1_COMM.getCommunicatorId()),
				content, "Freundschaftsanfrage wurde nicht verarbeitet. Sie sind bereits mit");
	}

	@Test
	void removeContactNoContactFail() {
		PacketContent content;
		final var clientOne = super.loginNextClient();
		final var clientData = clientOne.getCommunicatorData();

		content = new ContactRelationRequestDTO(EligibleContactEntity.CLIENT, clientData.getCommunicatorId(),
				MAX_1_COMM.getCommunicatorId(), clientData, false);
		TestResponseSingleClient.checkErrorResponse(clientOne, getClientEntity(MAX_1_COMM.getCommunicatorId()), content,
				"Freundschaftsanfrage wurde nicht verarbeitet. Sie sind nicht mit");
	}
}
