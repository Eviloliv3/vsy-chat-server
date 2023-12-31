package de.vsy.chat.server.server_test_helpers;

import de.vsy.server.data.ConnectionSpecifications;
import de.vsy.server.server_packet.packet_creation.ServerContentIdentificationProviderImpl;
import de.vsy.shared_module.packet_creation.NonStaticPacketCompiler;
import de.vsy.shared_module.packet_creation.OriginatingEntityProvider;
import de.vsy.shared_module.packet_management.PacketBuffer;
import de.vsy.shared_module.packet_management.ThreadPacketBufferLabel;
import de.vsy.shared_module.packet_management.ThreadPacketBufferManager;
import de.vsy.shared_module.packet_transmission.ConnectionThreadControl;
import de.vsy.shared_transmission.dto.CommunicatorDTO;
import de.vsy.shared_transmission.dto.authentication.AuthenticationDTO;
import de.vsy.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.packet.content.PacketContent;
import de.vsy.shared_transmission.packet.content.authentication.LoginRequestDTO;
import de.vsy.shared_transmission.packet.content.authentication.LoginResponseDTO;
import de.vsy.shared_transmission.packet.content.authentication.LogoutRequestDTO;
import de.vsy.shared_transmission.packet.content.authentication.LogoutResponseDTO;
import de.vsy.shared_transmission.packet.property.communicator.CommunicationEndpoint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.net.Socket;

import static de.vsy.chat.server.server_test_helpers.TestPacketVerifier.verifyPacketContent;
import static de.vsy.shared_transmission.dto.standard_empty_value.StandardEmptyDataProvider.EMPTY_AUTHENTICATION;
import static de.vsy.shared_transmission.dto.standard_empty_value.StandardEmptyDataProvider.EMPTY_COMMUNICATOR;
import static de.vsy.shared_transmission.packet.property.communicator.CommunicationEndpoint.getServerEntity;
import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_SERVER_ID;
import static de.vsy.shared_utility.standard_value.StandardStringProvider.STANDARD_EMPTY_STRING;

public class ClientConnection {

    protected static final Logger LOGGER = LogManager.getLogger();
    final int serverPort;
    boolean hasAuthenticationData;
    boolean authenticated;
    CommunicatorDTO clientData;
    AuthenticationDTO authenticationData;
    RequestPacketCreator requester;
    ThreadPacketBufferManager bufferManager;
    ConnectionThreadControl connectionControl;
    NonStaticPacketCompiler packetCompiler;
    Socket connectionSocket;

    public ClientConnection(int serverPort) throws IOException {
        this.serverPort = serverPort;
        prepareNewConnection();
    }

    private void prepareNewConnection() throws IOException {
        this.hasAuthenticationData = false;
        this.authenticated = false;
        setupNonStaticCompiler();
        setupThreadBufferManager();
        this.clientData = EMPTY_COMMUNICATOR;
        this.authenticationData = EMPTY_AUTHENTICATION;
        this.connectionSocket = new Socket(ConnectionSpecifications.getHostname(), this.serverPort);
        this.requester = new RequestPacketCreator(
                this.bufferManager.getPacketBuffer(ThreadPacketBufferLabel.OUTSIDE_BOUND), this.clientData);
        this.connectionControl = new ConnectionThreadControl(this.connectionSocket, this.bufferManager);
        this.connectionControl.initiateConnectionThreads();
    }

    private void setupNonStaticCompiler() {
        this.packetCompiler = new NonStaticPacketCompiler();
        this.packetCompiler.addContentIdentificationProvider(
                new ServerContentIdentificationProviderImpl());
        this.packetCompiler.addOriginatorEntityProvider(getOriginatorEntityProvider());
    }

    private void setupThreadBufferManager() {
        this.bufferManager = new ThreadPacketBufferManager();
        this.bufferManager.registerPacketBuffer(ThreadPacketBufferLabel.OUTSIDE_BOUND);
        this.bufferManager.registerPacketBuffer(ThreadPacketBufferLabel.SERVER_BOUND);
        this.bufferManager.registerPacketBuffer(ThreadPacketBufferLabel.HANDLER_BOUND);
    }

    private OriginatingEntityProvider getOriginatorEntityProvider() {
        return () -> CommunicationEndpoint.getClientEntity(clientData.getCommunicatorId());
    }

    public void setClientData(final AuthenticationDTO clientAuthenticationData,
                              final CommunicatorDTO clientCommunicatorData) {
        if (clientCommunicatorData == null || clientAuthenticationData == null) {
            LOGGER.warn("CommunicatorDTO or AuthenticationDTO missing. Wrong test results possible.");
        }

        this.clientData = clientCommunicatorData == null ? EMPTY_COMMUNICATOR : clientCommunicatorData;
        this.authenticationData = clientAuthenticationData == null ? EMPTY_AUTHENTICATION : clientAuthenticationData;
        ;

        if (this.clientData.getCommunicatorId() > 0) {
            this.authenticated = true;
        }

        if (!(this.authenticationData.getUsername().equals(STANDARD_EMPTY_STRING))) {
            this.hasAuthenticationData = true;
        }
    }

    public Packet sendRequest(PacketContent contentToSend, CommunicationEndpoint recipient) {
        final var request = this.packetCompiler.createRequest(recipient, contentToSend);

        this.requester.sendRequest(request);
        return request;
    }

    public Packet sendResponse(PacketContent contentToSend, Packet requestPacket) {
        final var response = this.packetCompiler.createResponse(contentToSend, requestPacket);

        this.requester.sendRequest(response);
        return response;
    }

    public AuthenticationDTO getAuthenticationData() {
        return this.authenticationData;
    }

    public boolean hasAuthenticationDataSet() {
        return this.hasAuthenticationData;
    }

    public void resetConnection() throws IOException {
        this.connectionControl.closeConnection();
        prepareNewConnection();
    }

    public boolean tryClientLogout() {
        Packet response;
        var logoutSuccess = false;

        if (this.authenticated) {
            requester.sendRequest(packetCompiler.createRequest(getServerEntity(STANDARD_SERVER_ID),
                    new LogoutRequestDTO(this.clientData)));
            do {
                response = readPacket();
            } while (response != null && !(response.getPacketContent() instanceof LogoutResponseDTO));

            if (response != null) {
                final var content = response.getPacketContent();

                if (content instanceof final LogoutResponseDTO logoutResponse) {
                    logoutSuccess = logoutResponse.getLogoutState();
                    this.authenticated = !logoutSuccess;
                    this.clientData = EMPTY_COMMUNICATOR;
                    this.authenticationData = EMPTY_AUTHENTICATION;
                    LOGGER.info("Logout successful");
                } else {
                    LOGGER.info("{}-Logout failed", this.clientData.getDisplayLabel());
                }
            } else {
                LOGGER.info(
                        "Logout failed. Letzte receivede Nachricht hatte nicht Typ \"LogoutResponseDTO\".");
            }
        } else {
            LOGGER.info("Logout unnötig. Es besteht keine Verbindung. ID: {}",
                    this.getCommunicatorData().getCommunicatorId());
        }
        return logoutSuccess;
    }

    public Packet readPacket() {
        long timeout = System.currentTimeMillis() + 2000;
        Packet readPacket = null;

        PacketBuffer handlerBoundBuffer = this.bufferManager.getPacketBuffer(
                ThreadPacketBufferLabel.HANDLER_BOUND);
        do {
            try {
                readPacket = handlerBoundBuffer.getPacket(250);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                LOGGER.info("Beim Holen des naechsten Pakets unterbrochen.");
                break;
            }
        } while (readPacket == null && System.currentTimeMillis() < timeout);
        return readPacket;
    }

    public CommunicatorDTO getCommunicatorData() {
        return CommunicatorDTO.valueOf(this.clientData.getCommunicatorId(),
                this.clientData.getDisplayLabel());
    }

    public boolean tryClientLogin() {
        Packet response;
        var loginSuccess = false;

        if (!this.authenticated && this.hasAuthenticationData) {
            requester.sendRequest(packetCompiler.createRequest(getServerEntity(STANDARD_SERVER_ID),
                    new LoginRequestDTO(this.authenticationData)));
            response = readPacket();
            verifyPacketContent(response, LoginResponseDTO.class);
            final var loginResponse = (LoginResponseDTO) response.getPacketContent();
            this.authenticated = loginSuccess = true;
            this.clientData = loginResponse.getClientData();
        } else {
            Assertions.fail("Login not possible. Credentials: " + this.hasAuthenticationData + " / authenticated: " + this.authenticated);
        }
        return loginSuccess;
    }

    public int getServerPort() {
        return this.serverPort;
    }
}
