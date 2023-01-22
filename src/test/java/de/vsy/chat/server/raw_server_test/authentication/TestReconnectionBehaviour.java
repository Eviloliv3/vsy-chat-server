/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package de.vsy.chat.server.raw_server_test.authentication;

import de.vsy.chat.server.raw_server_test.ServerPortProvider;
import de.vsy.chat.server.raw_server_test.ServerTestBase;
import de.vsy.chat.server.raw_server_test.TestClientDataProvider;
import de.vsy.chat.server.server_test_helpers.ClientConnection;
import de.vsy.chat.server.server_test_helpers.TestResponseSingleClient;
import de.vsy.shared_transmission.dto.CommunicatorDTO;
import de.vsy.shared_transmission.dto.authentication.AuthenticationDTO;
import de.vsy.shared_transmission.packet.content.PacketContent;
import de.vsy.shared_transmission.packet.content.authentication.ReconnectRequestDTO;
import de.vsy.shared_transmission.packet.content.authentication.ReconnectResponseDTO;
import de.vsy.shared_transmission.packet.content.error.ErrorDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static de.vsy.shared_transmission.packet.property.communicator.CommunicationEndpoint.getServerEntity;
import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_SERVER_ID;

/**
 *
 */
public class TestReconnectionBehaviour extends ServerTestBase {

    public TestReconnectionBehaviour(final ServerPortProvider clientConnectionPorts,
                                     final List<AuthenticationDTO> clientAuthenticationDataList) {
        super(clientConnectionPorts, clientAuthenticationDataList);
    }

    @Test
    void reconnectionFailAlreadyLoggedIn() {
        LOGGER.info("Test: reconnection -> failure: already authenticated on connection");
        PacketContent content;
        final var clientOne = super.loginNextClient();
        final var clientOneCommunicatorData = clientOne.getCommunicatorData();
        content = new ReconnectRequestDTO(clientOneCommunicatorData);
        TestResponseSingleClient.checkErrorResponse(clientOne, getServerEntity(STANDARD_SERVER_ID),
                content,
                "Request not processed. You are authenticated already.");
        LOGGER.info("Test:Reconnection -> failure: already authenticated on connection -- terminated");
    }


    @Test
    void reconnectionFailReconnectionUnderway() throws InterruptedException, IOException {
        LOGGER.info("Test: reconnection -> failure: reconnection attempt underway");
        ReconnectRequestDTO request;
        final ClientConnection clientOne, clientTwo;
        final CommunicatorDTO clientOneCommunicatorData;
        final AuthenticationDTO clientOneAuthenticationData;

        clientOne = super.loginNextClient();
        clientOneCommunicatorData = clientOne.getCommunicatorData();
        clientOneAuthenticationData = clientOne.getAuthenticationData();

        super.addConnectionNextServer();
        clientTwo = super.getUnusedClientConnection();

        clientOne.resetConnection();

        Thread.sleep(500);

        request = new ReconnectRequestDTO(clientOneCommunicatorData);
        clientTwo.sendRequest(request, getServerEntity(STANDARD_SERVER_ID));

        Thread.sleep(200);

        TestResponseSingleClient.checkErrorResponse(clientOne, getServerEntity(STANDARD_SERVER_ID),
                request, "Reconnect request failed, please try again.");
        final var response = clientTwo.readPacket();

        if (response.getPacketContent() instanceof final ReconnectResponseDTO reconnectResponse) {
            if (reconnectResponse.getReconnectionState()) {
                clientTwo.setClientData(clientOneAuthenticationData, clientOneCommunicatorData);
            } else {
                Assertions.fail("Reconnection attempt failed.");
            }
        } else {
            Assertions.fail("Response: " + ((ErrorDTO) response.getPacketContent()).getErrorMessage() +
                    ",  expected value: " + ReconnectResponseDTO.class.getSimpleName());
        }
        LOGGER.info("Test: reconnection -> failure: reconnection attempt underway -- terminated");
    }

    @Test
    void reconnectionFailStillLoggedIn() throws IOException {
        LOGGER.info("Test: reconnection -> failure: still logged on from another device");
        PacketContent content;
        final ClientConnection clientOne, clientTwo;
        final CommunicatorDTO clientOneCommunicatorData;

        clientOne = super.loginNextClient();
        clientOneCommunicatorData = clientOne.getCommunicatorData();

        super.addConnectionNextServer();
        clientTwo = super.getUnusedClientConnection();

        content = new ReconnectRequestDTO(clientOneCommunicatorData);
        TestResponseSingleClient.checkErrorResponse(clientTwo, getServerEntity(STANDARD_SERVER_ID),
                content, "Reconnect request failed, please try again.");
        LOGGER.info("Test: reconnection -> failure: still logged on from another device -- terminated");
    }

    @Test
    void reconnectionFailNotAuthenticated() {
        LOGGER.info("Test: reconnection -> failure: not authenticated and pending");
        PacketContent content;
        final var clientOne = super.getUnusedClientConnection();

        content = new ReconnectRequestDTO(TestClientDataProvider.FRANK_1_COMM);
        TestResponseSingleClient.checkErrorResponse(clientOne, getServerEntity(STANDARD_SERVER_ID),
                content,
                "You are not registered as authenticated.");
        LOGGER.info("Test: reconnection -> failure: not authenticated and pending -- terminated");
    }

    @Test
    void reconnectionFailMalformedData() {
        LOGGER.info("Test: reconnection -> failure: invalid data/token");
        PacketContent content;
        final var clientOne = super.getUnusedClientConnection();

        content = new ReconnectRequestDTO(CommunicatorDTO.valueOf(-4567, "Frank% Franke"));
        TestResponseSingleClient.checkErrorResponse(clientOne, getServerEntity(STANDARD_SERVER_ID),
                content,
                "Invalid communicator data:");
        LOGGER.info("Test: reconnection -> failure: invalid data/token -- terminated");
    }

    @Test
    void reconnectionFailFalseData() {
        LOGGER.info("Test: reconnection -> failure: unknown data/token");
        ReconnectRequestDTO erroneousRequest;
        final var clientOne = super.getUnusedClientConnection();

        erroneousRequest = new ReconnectRequestDTO(CommunicatorDTO.valueOf(123560, "Frank Wrong"));
        TestResponseSingleClient.checkErrorResponse(clientOne, getServerEntity(STANDARD_SERVER_ID),
                erroneousRequest,
                "There is no account with your credentials.");
        LOGGER.info("Test: reconnection -> failure: unknown data/token -- terminated");
    }

    @Test
    void reconnectionSuccess() throws InterruptedException, IOException {
        LOGGER.info("Test: reconnection -> success");
        ReconnectRequestDTO content;
        final ClientConnection clientOne, clientTwo;
        final CommunicatorDTO clientOneCommunicatorData;
        final AuthenticationDTO clientOneCredentials;

        clientOne = super.loginNextClient();
        super.addConnectionNextServer();
        clientTwo = super.getUnusedClientConnection();
        clientOneCommunicatorData = clientOne.getCommunicatorData();
        clientOneCredentials = clientOne.getAuthenticationData();

        clientOne.resetConnection();
        Thread.sleep(500);

        content = new ReconnectRequestDTO(clientOneCommunicatorData);
        clientTwo.sendRequest(content, getServerEntity(STANDARD_SERVER_ID));
        final var response = clientTwo.readPacket();

        if (response.getPacketContent() instanceof final ReconnectResponseDTO reconnectResponse) {
            if (reconnectResponse.getReconnectionState()) {
                clientTwo.setClientData(clientOneCredentials, clientOneCommunicatorData);
            } else {
                Assertions.fail("Reconnection attempt failed.");
            }
        } else {
            Assertions.fail("Response: " + ((ErrorDTO) response.getPacketContent()).getErrorMessage() +
                    ",  expected value: " + ReconnectResponseDTO.class.getSimpleName());
        }
        LOGGER.info("Test: reconnection -> success -- terminated");
    }
}
