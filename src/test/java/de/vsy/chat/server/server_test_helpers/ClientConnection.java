package de.vsy.chat.server.server_test_helpers;

import de.vsy.server.server.data.ConnectionSpecifications;
import de.vsy.server.server_packet.packet_creation.ServerContentIdentificationProviderImpl;
import de.vsy.shared_module.shared_module.packet_creation.NonStaticPacketCompiler;
import de.vsy.shared_module.shared_module.packet_creation.OriginatingEntityProvider;
import de.vsy.shared_module.shared_module.packet_management.PacketBuffer;
import de.vsy.shared_module.shared_module.packet_management.ThreadPacketBufferLabel;
import de.vsy.shared_module.shared_module.packet_management.ThreadPacketBufferManager;
import de.vsy.shared_module.shared_module.packet_transmission.ConnectionThreadControl;
import de.vsy.shared_transmission.shared_transmission.dto.CommunicatorDTO;
import de.vsy.shared_transmission.shared_transmission.dto.authentication.AuthenticationDTO;
import de.vsy.shared_transmission.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.shared_transmission.packet.content.PacketContent;
import de.vsy.shared_transmission.shared_transmission.packet.content.authentication.LoginRequestDTO;
import de.vsy.shared_transmission.shared_transmission.packet.content.authentication.LoginResponseDTO;
import de.vsy.shared_transmission.shared_transmission.packet.content.authentication.LogoutRequestDTO;
import de.vsy.shared_transmission.shared_transmission.packet.content.authentication.LogoutResponseDTO;
import de.vsy.shared_transmission.shared_transmission.packet.content.error.ErrorDTO;
import de.vsy.shared_transmission.shared_transmission.packet.property.communicator.CommunicationEndpoint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.Socket;

import static de.vsy.shared_transmission.shared_transmission.dto.standard_empty_value.StandardEmptyDataProvider.EMPTY_AUTHENTICATION;
import static de.vsy.shared_transmission.shared_transmission.dto.standard_empty_value.StandardEmptyDataProvider.EMPTY_COMMUNICATOR;
import static de.vsy.shared_transmission.shared_transmission.packet.property.communicator.CommunicationEndpoint.getServerEntity;
import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_SERVER_ID;
import static de.vsy.shared_utility.standard_value.StandardStringProvider.STANDARD_EMPTY_STRING;

public
class ClientConnection {

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

    public
    ClientConnection (int serverPort)
    throws IOException {
        this.serverPort = serverPort;
        prepareNewConnection();
    }

    private
    void prepareNewConnection ()
    throws IOException {
        this.hasAuthenticationData = false;
        this.authenticated = false;
        setupNonStaticCompiler();
        setupThreadBufferManager();
        this.clientData = EMPTY_COMMUNICATOR;
        this.authenticationData = EMPTY_AUTHENTICATION;
        this.connectionSocket = new Socket(ConnectionSpecifications.getHostname(),
                                           this.serverPort);
        this.requester = new RequestPacketCreator(this.bufferManager.getPacketBuffer(
                ThreadPacketBufferLabel.OUTSIDE_BOUND), this.clientData);
        this.connectionControl = new ConnectionThreadControl(this.connectionSocket,
                                                             this.bufferManager);
        this.connectionControl.initiateConnectionThreads();
    }

    private
    void setupNonStaticCompiler () {
        this.packetCompiler = new NonStaticPacketCompiler();
        this.packetCompiler.addContentIdentificator(
                new ServerContentIdentificationProviderImpl());
        this.packetCompiler.addOriginatorEntityProvider(
                getOriginatorEntityProvider());
    }

    private
    void setupThreadBufferManager () {
        this.bufferManager = new ThreadPacketBufferManager();
        this.bufferManager.registerPacketBuffer(
                ThreadPacketBufferLabel.OUTSIDE_BOUND);
        this.bufferManager.registerPacketBuffer(
                ThreadPacketBufferLabel.SERVER_BOUND);
        this.bufferManager.registerPacketBuffer(
                ThreadPacketBufferLabel.HANDLER_BOUND);
    }

    private
    OriginatingEntityProvider getOriginatorEntityProvider () {
        return () -> CommunicationEndpoint.getClientEntity(
                clientData.getCommunicatorId());
    }

    public
    void setClientData (final AuthenticationDTO clientAuthenticationData,
                        final CommunicatorDTO clientCommunicatorData) {
        final AuthenticationDTO clientAuthentication;
        final CommunicatorDTO clientData;
        if (clientCommunicatorData == null || clientAuthenticationData == null) {
            LOGGER.warn("Keine Klientendaten angegeben. Möglicherweise werden " +
                        "nachfolgende Testergebnisse verfälscht.");
        }

        clientAuthentication = clientAuthenticationData ==
                               null ? EMPTY_AUTHENTICATION : clientAuthenticationData;
        clientData = clientCommunicatorData ==
                     null ? EMPTY_COMMUNICATOR : clientCommunicatorData;

        this.clientData = clientData;
        this.authenticationData = clientAuthentication;

        if (this.clientData.getCommunicatorId() > 0) {
            this.authenticated = true;
        }

        if (!this.authenticationData.getLogin().equals(STANDARD_EMPTY_STRING)) {
            this.hasAuthenticationData = true;
        }
    }

    public
    Packet sendRequest (PacketContent contentToSend,
                        CommunicationEndpoint recipient) {
        final var request = this.packetCompiler.createRequest(recipient,
                                                              contentToSend);

        this.requester.request(request);
        return request;
    }

    public
    Packet sendResponse (PacketContent contentToSend, Packet requestPacket) {
        final var response = this.packetCompiler.createResponse(contentToSend,
                                                                requestPacket);

        this.requester.request(response);
        return response;
    }

    public
    AuthenticationDTO getAuthenticationData () {
        return this.authenticationData;
    }

    public
    boolean hasAuthenticationDataSet () {
        return this.hasAuthenticationData;
    }

    public
    void resetConnection ()
    throws IOException {

        if (this.hasAuthenticationData || this.authenticated) {
            this.connectionControl.closeConnection();
            prepareNewConnection();
        } else {
            LOGGER.info("Verbindung wurde nicht gestartet.");
        }
    }

    public
    boolean tryClientLogout () {
        Packet response;
        var logoutSuccess = false;

        if (this.authenticated) {
            requester.request(
                    packetCompiler.createRequest(getServerEntity(STANDARD_SERVER_ID),
                                                 new LogoutRequestDTO(
                                                         this.clientData)));
            do {
                response = readPacket();
            } while (response != null &&
                     !(response.getPacketContent() instanceof LogoutResponseDTO));

            if (response != null) {
                final var content = response.getPacketContent();

                if (content instanceof final LogoutResponseDTO logoutResponse) {
                    logoutSuccess = logoutResponse.getLogoutState();
                    LOGGER.info("Logout erfolgreich");
                } else {
                    LOGGER.info("{}-Logout fehlgeschlagen",
                                this.clientData.getDisplayLabel());
                }
            } else {
                LOGGER.info(
                        "Logout fehlgeschlagen. Letzte empfangene Nachricht hatte nicht Typ \"LogoutResponseDTO\".");
            }
        } else {
            LOGGER.info("Logout unnötig. Es besteht keine Verbindung. ID: {}",
                        this.getCommunicatorData().getCommunicatorId());
        }

        return logoutSuccess;
    }

    public
    Packet readPacket () {
        long timeout = System.currentTimeMillis() + 2000L;
        Packet readPacket = null;

        PacketBuffer handlerBoundBuffer = this.bufferManager.getPacketBuffer(
                ThreadPacketBufferLabel.HANDLER_BOUND);
        do {
            try {
                readPacket = handlerBoundBuffer.getPacket();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                LOGGER.info("Beim Holen des naechsten Pakets unterbrochen.");
                break;
            }
        } while (readPacket == null);// && System.currentTimeMillis() < timeout);
        return readPacket;
    }

    public
    CommunicatorDTO getCommunicatorData () {
        return CommunicatorDTO.valueOf(this.clientData.getCommunicatorId(),
                                       this.clientData.getDisplayLabel());
    }

    public
    boolean tryClientLogin () {
        Packet response;
        var loginSuccess = false;

        if (!this.authenticated && this.hasAuthenticationData) {
            requester.request(
                    packetCompiler.createRequest(getServerEntity(STANDARD_SERVER_ID),
                                                 new LoginRequestDTO(
                                                         this.authenticationData)));
            response = readPacket();

            if (response != null) {
                final var content = response.getPacketContent();

                if (content instanceof final LoginResponseDTO loginResponse) {
                    this.authenticated = loginSuccess = true;
                    this.clientData = loginResponse.getClientData();

                    LOGGER.info("{}-Login erfolgreich",
                                this.clientData.getDisplayLabel());
                } else {
                    LOGGER.info("{}-Login fehlgeschlagen. Antworttyp " +
                                "statt LoginResponseDTO: {}",
                                this.authenticationData.getLogin(),
                                content.getClass().getSimpleName());
                    if(content instanceof ErrorDTO errorResponse){
                        LOGGER.info(errorResponse.getErrorMessage());
                    }
                }
            } else {
                LOGGER.info("{}-Login fehlgeschlagen. Keine Antwort " + "erhalten.",
                            this.authenticationData.getLogin());
            }
        } else {
            LOGGER.info(
                    "Login nicht möglich. Daten vorhanden: {} | Bereits eingeloggt: {}.",
                    this.hasAuthenticationData, this.authenticated);
        }
        return loginSuccess;
    }
}
