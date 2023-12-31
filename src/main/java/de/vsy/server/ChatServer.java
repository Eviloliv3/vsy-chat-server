package de.vsy.server;

import de.vsy.server.client_handling.ClientConnectionHandler;
import de.vsy.server.client_handling.data_management.logic.ClientStateDistributor;
import de.vsy.server.client_handling.data_management.logic.ClientStatePublisher;
import de.vsy.server.client_handling.data_management.logic.ClientSubscriptionHandler;
import de.vsy.server.client_management.ClientStateTranslator;
import de.vsy.server.data.ConnectionSpecifications;
import de.vsy.server.data.ServerDataManager;
import de.vsy.server.data.ServerPersistentDataManager;
import de.vsy.server.data.access.HandlerAccessManager;
import de.vsy.server.data.socketConnection.LocalServerConnectionData;
import de.vsy.server.server_connection.ClientConnectionEstablisher;
import de.vsy.server.server_connection.ClientServer;
import de.vsy.server.server_packet.packet_creation.ServerContentIdentificationProviderImpl;
import de.vsy.server.service.Service;
import de.vsy.server.service.ServiceControl;
import de.vsy.server.service.ServiceHealthMonitor;
import de.vsy.shared_module.packet_creation.PacketCompiler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;

import static de.vsy.server.data.socketConnection.SocketConnectionState.INITIATED;
import static de.vsy.shared_transmission.packet.property.communicator.CommunicationEndpoint.getServerEntity;
import static de.vsy.shared_utility.standard_value.ThreadContextValues.*;
import static java.util.Arrays.asList;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;

public class ChatServer implements ClientServer {

    private static final Logger LOGGER = LogManager.getLogger();
    private final ExecutorService clientConnectionPool;
    private final Timer serviceMonitor;
    private ServerDataManager serverDataModel;
    private ServerPersistentDataManager serverPersistentDataManager;
    private ServiceControl serviceControl;
    private ClientConnectionEstablisher clientConnectionEstablisher;

    /**
     * Instantiates a new chat server.
     */
    public ChatServer() {
        this.clientConnectionPool = newCachedThreadPool();
        this.serviceMonitor = new Timer("ServiceHealthMonitor");
    }

    /**
     * The main method.
     *
     * @param args the arguments
     */
    public static void main(final String[] args) {
        final var server = new ChatServer();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                server.shutdownServer();
            } catch (RuntimeException re) {
                LOGGER.error("{}:{}\n{}", re.getClass().getSimpleName(), re.getMessage(),
                        asList(re.getStackTrace()));
                LogManager.shutdown();
            }
        }));
        Thread.currentThread().setName("ChatServer");
        ThreadContext.put(LOG_ROUTE_CONTEXT_KEY, STANDARD_SERVER_ROUTE_VALUE);
        server.serve();
        LOGGER.trace("Server will be shutdown regularly.");
        //server.shutdownServer();
        ThreadContext.clearAll();
    }

    /**
     * Prepare server.
     *
     * @throws IllegalStateException if no ServerSocket could be initiated.
     */
    private void prepareServer() {
        String serverThreadName;
        final var localServerConnectionData = setupServerSocket();
        final var serverPrepared = localServerConnectionData != null;

        if (!serverPrepared) {
            throw new IllegalStateException("No specified port usable. Server cannot be started.");
        }
        serverThreadName = "ChatServer[" + localServerConnectionData.getServerId() + ":"
                + localServerConnectionData.getServerPort() + "]";
        ThreadContext.put(LOG_FILE_CONTEXT_KEY, serverThreadName);

        setupDataManager(localServerConnectionData);
        setupPersistentDataAccess();
        setupStaticServerDataAccess();
        setupAndStartServices();
    }

    public void serve() {
        prepareServer();
        clientConnectionEstablisher = new ClientConnectionEstablisher(
                this.serverDataModel.getServerConnectionDataManager().getLocalServerConnectionData(), this);
        clientConnectionEstablisher.acceptClientConnections();
    }

    @Override
    public void shutdownServer() {
        LOGGER.info("Server shutdown initiated. Interruption status: {}",
                Thread.interrupted());
        final var pendingBufferWatcher = HandlerAccessManager.getPendingClientWatcherManager();

        this.clientConnectionEstablisher.stopEstablishingConnections();
        LOGGER.info("Client connection establisher terminated.");
        this.clientConnectionPool.shutdownNow();
        LOGGER.info("Client handler pool shutdown initiated.");
        pendingBufferWatcher.shutdownPendingBufferWatchers();
        LOGGER.info("Pending client watcher pool shutdown initiated.");
        this.serviceMonitor.cancel();
        this.serviceMonitor.purge();
        LOGGER.info("Service monitor shutdown.");
        this.serviceControl.stopAllServices();
        LOGGER.info("Services shutdown.");

        try {
            var clientConnectionsDown = this.clientConnectionPool.awaitTermination(5, SECONDS);

            if (clientConnectionsDown) {
                LOGGER.info("Client handler pool shutdown.");
            } else {
                LOGGER.error(
                        "Client connection pool shutdown unexpectedly took more than 5 seconds and may be deadlocked.");
            }
        } catch (InterruptedException ie) {
            LOGGER.error("Interrupted while waiting for client handler pool to terminate.");
        }

        try {
            pendingBufferWatcher.awaitPendingBufferWatchers();
        } catch (InterruptedException e) {
            LOGGER.error(
                    "Interrupted while waiting for client pending buffer watcher pool to terminate.");
        }

        if (this.serverDataModel.getServerConnectionDataManager().getServerConnections(
                INITIATED).isEmpty()) {
            this.serverPersistentDataManager.getClientStateAccessManager().removeAllClientStates();
            LOGGER.info("Last remaining registered server. Persisted client states will be removed.");
        }
        this.serverDataModel.getServerConnectionDataManager().closeAllConnections();
        LOGGER.info("All sockets closed.");
        serverPersistentDataManager.removePersistentAccess();
        LOGGER.info("Persistent data access removed.");
        LOGGER.info("Server shutdown completed.");
    }

    /**
     * Setup server socket.
     *
     * @return the server socket
     */
    private LocalServerConnectionData setupServerSocket() {
        LocalServerConnectionData localServerConnectionData = null;
        final var serverPorts = ConnectionSpecifications.getServerPorts();
        final var hostname = ConnectionSpecifications.getHostname();

        for (var currentPortNumber : serverPorts) {

            if (!serverListening(hostname, currentPortNumber)) {

                try {
                    ServerSocket masterSocket = new ServerSocket(currentPortNumber);
                    LOGGER.info("Server will accept connections on port {}", currentPortNumber);
                    localServerConnectionData = LocalServerConnectionData.valueOf(masterSocket.getLocalPort(),
                            masterSocket);
                    break;
                } catch (final IOException ioe) {
                    LOGGER.error("ServerSocket could not be initiated on port {}. " + "{}: {}",
                            currentPortNumber, ioe.getClass().getSimpleName(), ioe.getMessage());
                }
            }
        }
        return localServerConnectionData;
    }

    private void setupDataManager(LocalServerConnectionData localServerConnectionData) {
        this.serverDataModel = new ServerDataManager(localServerConnectionData);
    }

    private void setupPersistentDataAccess() {
        this.serverPersistentDataManager = new ServerPersistentDataManager(
                this.serverDataModel.getServerConnectionDataManager());
        this.serverPersistentDataManager.initiatePersistentAccess();
    }

    private void setupStaticServerDataAccess() {
        final var serverConnectionDataManager = this.serverDataModel.getServerConnectionDataManager();
        PacketCompiler.addOriginatorEntityProvider(
                () -> getServerEntity(
                        serverConnectionDataManager.getLocalServerConnectionData().getServerId()));
        PacketCompiler.addContentIdentificationProvider(new ServerContentIdentificationProviderImpl());
        HandlerAccessManager.setupStaticAccess(this.serverDataModel, this.serverPersistentDataManager);
        ClientSubscriptionHandler.setupStaticServerDataAccess();
        ClientStatePublisher.setupStaticServerDataAccess(serverConnectionDataManager);
        ClientStateDistributor.setupStaticServerDataAccess(
                this.serverPersistentDataManager.getClientStateAccessManager(),
                serverConnectionDataManager.getLocalServerConnectionData());
    }

    private void setupAndStartServices() {
        final TimerTask serviceMonitoring;
        this.serviceControl = new ServiceControl(this.serverDataModel,
                this.serverPersistentDataManager);
        this.serviceControl.startInterServerConnector();
        this.serviceControl.startServices();
        serviceMonitoring = new ServiceHealthMonitor(this, this.serviceControl);
        this.serviceMonitor.scheduleAtFixedRate(serviceMonitoring, 100, 500);

        /* add remotely connected clients to publish/subscribe network */
        waitForPrecedingServerConnections();
        loadRemoteClientConnections();
        /*
         * server synchronization state set
         */
    }

    /**
     * Server listening.
     *
     * @param host the host
     * @param port the port
     * @return true, if successful
     * @throws IllegalArgumentException when combination of host and port does not result in correctly
     *                                  formatted internet address
     */
    private boolean serverListening(final String host, final int port) {
        final var listening = true;

        try (var ignored = new Socket(host, port)) {
            return listening;
        } catch (final UnknownHostException uhe) {
            final var errorMessage = "Unknown combination host/port: " + host + "/" + port;
            throw new IllegalArgumentException(errorMessage, uhe);
        } catch (final IOException ioe) {
            LOGGER.info("Combination host/port: {}/{} usable.", host, port);
            return !listening;
        }
    }

    private void waitForPrecedingServerConnections() {
        LOGGER.info("Waiting for connection synchronization with existing servers started.");

        try {
            this.serverDataModel.getServerConnectionDataManager().waitForUninitiatedConnections();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error(
                    "Interrupted while waiting for preceding server connections to be established.");
        }
        LOGGER.info("Connection synchronization with existing servers finished.");
    }

    /**
     * Loads all existing remotely connected client states. Checks for each remote server
     * PacketBuffer, if there are client connections and uses the server PacketBuffer for the client
     * subscriptions. Extra subscriptions per client will also be subscribed to using the remote
     * server PacketBuffer.
     */
    private void loadRemoteClientConnections() {
        LOGGER.info("Remote client states will be loaded.");

        final var clientSubscriptions = this.serverDataModel.getClientCategorySubscriptionManager();
        final var synchronizedConnections = this.serverDataModel.getServerConnectionDataManager()
                .getServerConnections(INITIATED);
        final var activeClients = this.serverPersistentDataManager.getClientStateAccessManager()
                .getAllActiveClientStates();

        if (!activeClients.isEmpty()) {

            for (final var remoteServerData : synchronizedConnections) {
                final var remoteServerId = remoteServerData.getServerId();
                final var remoteServerBuffer = remoteServerData.getRemoteServerBuffer();

                for (final var clientEntry : activeClients.entrySet()) {
                    final var clientState = clientEntry.getValue();

                    if (clientState.getServerId() == remoteServerId) {
                        final var extraSubscriptions = clientState.getExtraSubscriptions();
                        final var subscriptionMapping = ClientStateTranslator.prepareClientSubscriptionMap(
                                clientState.getCurrentState(), true, clientEntry.getKey());
                        subscriptionMapping.putAll(extraSubscriptions);

                        for (var topicSubscriptionSet : subscriptionMapping.entrySet()) {
                            final var topic = topicSubscriptionSet.getKey();

                            for (var thread : topicSubscriptionSet.getValue()) {
                                clientSubscriptions.subscribe(topic, thread, remoteServerBuffer);
                            }
                        }
                    }
                }
            }
        }
        this.serverDataModel.getServerSynchronizationManager().clientSynchronizationComplete();
        LOGGER.info("Remote client states loaded.");
    }

    @Override
    public void serveClient(Socket clientConnectionSocket) {

        if (clientConnectionSocket != null) {
            final var requestAssignmentBuffer = this.serverDataModel.getServicePacketBufferManager()
                    .getRandomBuffer(Service.TYPE.REQUEST_ROUTER);
            this.clientConnectionPool.execute(
                    new ClientConnectionHandler(clientConnectionSocket, requestAssignmentBuffer));
        } else {
            LOGGER.error("null-socket cannot used to serve.");
        }
    }
}
