package de.vsy.chat.server.raw_server_test.relation;

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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static de.vsy.shared_transmission.shared_transmission.packet.property.communicator.CommunicationEndpoint.getClientEntity;
import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_SERVER_ID;

public
class TestClientRelationChanges extends ServerTestBase {

    public
    TestClientRelationChanges (final ServerPortProvider clientConnectionPorts,
                               final List<AuthenticationDTO> clientAuthenticationDataList) {
        super(clientConnectionPorts, clientAuthenticationDataList);
    }
/*
    @Test
    void addContactSuccess ()
    throws IOException {
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

        content = new ContactRelationRequestDTO(EligibleContactEntity.CLIENT,
                                                clientOneData.getCommunicatorId(),
                                                15003, clientOneData, true);

        clientOne.sendRequest(content, getClientEntity(15003));
        packet = clientTwo.readPacket();

        if (packet == null ||
            !(packet.getPacketContent() instanceof ContactRelationRequestDTO)) {
            Assertions.fail("Keine Antwort ContactRelationRequestDTO empfangen.");
        } else {
            content = packet.getPacketContent();
        }

        content = new ContactRelationResponseDTO(clientTwoData, true,
                                                 (ContactRelationRequestDTO) content);
        clientTwo.sendResponse(content, packet);
        packet = clientOne.readPacket();

        if (packet == null) {
            Assertions.fail("Keine Antwort ContactRelationResponseDTO empfangen.");
        }
        Assertions.assertInstanceOf(ContactRelationResponseDTO.class, content);
    }
*/
    @Test
    void removeContactSuccess ()
    throws IOException {
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

        content = new ContactRelationRequestDTO(EligibleContactEntity.CLIENT,
                                                clientOneData.getCommunicatorId(),
                                                15004, clientOneData, false);
        clientOne.sendRequest(content, getClientEntity(15004));
        packet = clientTwo.readPacket();

        if (packet != null) {
            content = packet.getPacketContent();
        } else {
            Assertions.fail("Keine Antwort ContactRelationRequestDTO empfangen.");
        }
        content = new ContactRelationResponseDTO(clientTwoData, false,
                                                 (ContactRelationRequestDTO) content);
        clientTwo.sendResponse(content, packet);
        packet = clientOne.readPacket();

        if (packet != null) {
            content = packet.getPacketContent();
        } else {
            Assertions.fail("Keine Antwort ContactRelationResponseDTO empfangen.");
        }
        Assertions.assertInstanceOf(ContactRelationResponseDTO.class, content);
    }
/*
    @Test
    void contactRelationAddMissingFail () {
        PacketContent content;
        final var clientOne = super.loginNextClient();
        final var clientData = clientOne.getCommunicatorData();

        content = new ContactRelationRequestDTO(EligibleContactEntity.CLIENT,
                                                clientData.getCommunicatorId(),
                                                STANDARD_SERVER_ID, null, true);
        TestResponseSingleClient.checkErrorResponse(clientOne, getClientEntity(
                                                            STANDARD_SERVER_ID), content,
                                                    "Ungültige Kontaktanfrage. Fehlerhafte Kommunikatordaten: Es sind keine Kommunikatordaten vorhanden.");
    }

    @Test
    void addContactOfflineFail () {
        PacketContent content;
        final var clientOne = super.loginNextClient();
        final var clientData = clientOne.getCommunicatorData();

        content = new ContactRelationRequestDTO(EligibleContactEntity.CLIENT,
                                                clientData.getCommunicatorId(),
                                                15003, clientData, true);

        TestResponseSingleClient.checkErrorResponse(clientOne,
                                                    getClientEntity(15003), content,
                                                    "Das Paket wurde nicht zugestellt. Paket wurde nicht zugestellt. Kontakt offline.");
    }

    @Test
    void addContactAlreadyFriendsFail () {
        PacketContent content;
        final var clientOne = super.loginNextClient();
        final var clientData = clientOne.getCommunicatorData();

        content = new ContactRelationRequestDTO(EligibleContactEntity.CLIENT,
                                                clientData.getCommunicatorId(),
                                                15002, clientData, true);
        TestResponseSingleClient.checkErrorResponse(clientOne,
                                                    getClientEntity(15002), content,
                                                    "Freundschaftsanfrage wurde nicht verarbeitet. Sie sind bereits mit");
    }

    @Test
    void removeContactNoContactFail () {
        PacketContent content;
        final var clientOne = super.loginNextClient();
        final var clientData = clientOne.getCommunicatorData();

        content = new ContactRelationRequestDTO(EligibleContactEntity.CLIENT,
                                                clientData.getCommunicatorId(),
                                                15005, clientData, false);
        TestResponseSingleClient.checkErrorResponse(clientOne,
                                                    getClientEntity(15005), content,
                                                    "Freundschaftsanfrage wurde nicht verarbeitet. Sie sind nicht mit");
    }

 */
}
