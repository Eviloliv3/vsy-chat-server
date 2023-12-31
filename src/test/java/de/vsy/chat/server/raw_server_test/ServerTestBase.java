package de.vsy.chat.server.raw_server_test;

import de.vsy.chat.server.server_test_helpers.ClientConnection;
import de.vsy.shared_transmission.dto.authentication.AuthenticationDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static de.vsy.shared_utility.standard_value.ThreadContextValues.LOG_ROUTE_CONTEXT_KEY;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ServerTestBase {

    protected static final Logger LOGGER = LogManager.getLogger();
    protected final ServerPortProvider portProvider;
    protected final List<AuthenticationDTO> clientAuthenticationDataList;
    protected final List<ClientConnection> clientConnectionList;
    protected final Set<AuthenticationDTO> activeClientAuthenticationData;

    public ServerTestBase(ServerPortProvider clientConnectionPorts,
                          List<AuthenticationDTO> clientAuthenticationDataList) {
        ThreadContext.put(LOG_ROUTE_CONTEXT_KEY, "test");
        this.portProvider = clientConnectionPorts;
        this.clientAuthenticationDataList = clientAuthenticationDataList;
        this.clientConnectionList = new ArrayList<>(6);
        this.activeClientAuthenticationData = new HashSet<>();
    }

    @BeforeEach
    void createSingleConnection() throws IOException {
        this.addConnectionNextServer();
    }

    /**
     * Connection object that will attempt connecting to a different server than the previously
     * created connection object, will be generated. That bare object can subsequently be gotten hold
     * of using the <ref>getUnusedClientConnection()
     * </ref>-method. The connection object will still need to be setup
     * subsequently. If there was only one server port specified, the connection will try connecting
     * to the existing server.
     */
    protected void addConnectionNextServer() throws IOException {
        final var clientConnection = new ClientConnection(portProvider.getNextServerPort());
        addClientConnection(clientConnection);
    }

    protected void addClientConnection(final ClientConnection connection) {
        clientConnectionList.add(connection);
    }

    /**
     * Connection object that will attempt connecting to the same server as the previously created
     * connection object, will be generated. That bare object can subsequently be gotten hold of using
     * the <ref>getUnusedClientConnection()
     * </ref>-method. The connection object will still need to be setup
     * subsequently.
     */
    protected void addConnectionSameServer() throws IOException {
        final var clientConnection = new ClientConnection(portProvider.getCurrentServerPort());
        addClientConnection(clientConnection);
    }

    @AfterEach
    protected void breakDownServerConnection() throws InterruptedException, IOException {
        this.resetConnections();
        Thread.sleep(500);
    }

    @AfterAll
    protected void resetConnections() {

        for (var currentConnection : this.clientConnectionList) {
            AuthenticationHelper.logoutClient(currentConnection);
        }
        this.activeClientAuthenticationData.clear();
        this.clientConnectionList.clear();
    }

    /**
     * Tries to get an unused set of credentials and will proceed with the login process by calling
     * <ref>loginNextClient(AuthenticationDTO)</ref>, if a set of unused credentials was specified.
     * Also requires a setup unused ClientConnection object. That can be created calling one of the
     * addConnection****Server()methods
     *
     * @return the ClientConnection with logged in client, or null if no unused credentials or no
     * setup, unused ClientConnection object could be specified.
     */
    protected ClientConnection loginNextClient() {
        ClientConnection connection = null;
        final var clientAuthenticationData = getUnusedAuthenticationData();

        if (clientAuthenticationData != null) {
            connection = loginNextClient(clientAuthenticationData);
        } else {
            Assertions.fail("Login failed. No valid data found.");
        }
        return connection;
    }

    protected AuthenticationDTO getUnusedAuthenticationData() {

        for (var currentAuthenticationData : this.clientAuthenticationDataList) {
            if (!this.activeClientAuthenticationData.contains(currentAuthenticationData)) {
                return currentAuthenticationData;
            }
        }
        return null;
    }

    protected ClientConnection loginNextClient(AuthenticationDTO clientAuthenticationData) {
        LOGGER.info("Login attempt initiated.");
        var clientConnection = this.getUnusedClientConnection();

        if (clientConnection == null) {
            Assertions.fail("Login failed. No usable connection -> " + clientAuthenticationData);
        }
        clientConnection = AuthenticationHelper.loginSpecificClient(clientConnection, clientAuthenticationData);

        if (clientConnection == null) {
            Assertions.fail("Login failed for: " + clientAuthenticationData);
        }
        this.activeClientAuthenticationData.add(clientAuthenticationData);
        LOGGER.info("Login attempt successful.");
        return clientConnection;
    }

    protected ClientConnection getUnusedClientConnection() {

        for (var currentConnection : this.clientConnectionList) {
            if (!currentConnection.hasAuthenticationDataSet()) {
                return currentConnection;
            }
        }
        return null;
    }
}
