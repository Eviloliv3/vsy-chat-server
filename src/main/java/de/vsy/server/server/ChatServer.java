package de.vsy.server.server;

import static de.vsy.shared_transmission.shared_transmission.packet.property.communicator.CommunicationEndpoint.getServerEntity;
import static de.vsy.shared_utility.standard_value.ThreadContextValues.LOG_FILE_CONTEXT_KEY;
import static de.vsy.shared_utility.standard_value.ThreadContextValues.LOG_ROUTE_CONTEXT_KEY;
import static de.vsy.shared_utility.standard_value.ThreadContextValues.STANDARD_SERVER_ROUTE_VALUE;
import static java.util.concurrent.Executors.newFixedThreadPool;

import de.vsy.server.client_handling.ClientConnectionHandler;
import de.vsy.server.client_handling.data_management.logic.ClientStatePublisher;
import de.vsy.server.client_handling.data_management.logic.ClientStateRecorder;
import de.vsy.server.client_handling.data_management.logic.ClientSubscriptionHandler;
import de.vsy.server.server.client_management.ClientStateTranslator;
import de.vsy.server.server.data.ConnectionSpecifications;
import de.vsy.server.server.data.ServerDataManager;
import de.vsy.server.server.data.ServerPersistentDataManager;
import de.vsy.server.server.data.access.HandlerAccessManager;
import de.vsy.server.server.server_connection.ClientConnectionEstablisher;
import de.vsy.server.server.server_connection.LocalServerConnectionData;
import de.vsy.server.server_packet.packet_creation.ServerContentIdentificationProviderImpl;
import de.vsy.server.service.Service;
import de.vsy.server.service.ServiceControl;
import de.vsy.server.service.ServiceHealthMonitor;
import de.vsy.shared_module.shared_module.packet_creation.PacketCompiler;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

public class ChatServer implements ClientServer {

  private static final Logger LOGGER = LogManager.getLogger();
  private final ExecutorService connectionPool;
  private final Timer serviceMonitor;
  private ServerDataManager serverDataModel;
  private ServerPersistentDataManager serverPersistentDataManager;
  private ServiceControl serviceControl;
  private ClientConnectionEstablisher connectionEstablisher;

  /**
   * Instantiates a new chat server.
   */
  public ChatServer() {
    this.connectionPool = newFixedThreadPool(10);
    Runtime.getRuntime().addShutdownHook(new Thread(Thread.currentThread()::interrupt));
    this.serviceMonitor = new Timer("ServiceHealthMonitor");
  }

  /**
   * The main method.
   *
   * @param args the arguments
   */
  public static void main(final String[] args) {
    final var server = new ChatServer();
    Thread.currentThread().setName("Chatserver");
    ThreadContext.put(LOG_ROUTE_CONTEXT_KEY, STANDARD_SERVER_ROUTE_VALUE);

    server.serve();
    server.shutdownServer();
    ThreadContext.clearAll();
    System.exit(0);
  }

  /**
   * Prepare server.
   *
   * @return true, if successful
   * @throws IllegalStateException if no ServerSocket could be initiated.
   */
  private void prepareServer() {
    String serverThreadName;
    /* ServerSocket starten, wenn Datenzugriff besteht. */
    final var localServerConnectionData = setupServerSocket();
    final var serverPrepared = localServerConnectionData != null;

    if (!serverPrepared) {
      throw new IllegalStateException(
          "Keiner der vorgegebenen Ports ist mehr frei. Der Server "
              + "kann somit nicht gestartet werden.");
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
    connectionEstablisher = new ClientConnectionEstablisher(
        this.serverDataModel.getServerConnectionDataManager().getLocalServerConnectionData(), this);
    connectionEstablisher.startAcceptingClientConnections();
  }

  /**
   * Shutdown server.
   */
  @Override
  public void shutdownServer() {
    LOGGER.info("Server wird heruntergefahren. Unterbrechungsstatus: {}",
        Thread.currentThread().isInterrupted());
    this.connectionEstablisher.changeServerHealthFlag(false);
    this.serviceMonitor.cancel();
    this.serviceMonitor.purge();

    do {
      LOGGER.info("Es wird auf KlientenHandler gewartet.");
      Thread.yield();
    } while (!this.connectionPool.isTerminated());
    LOGGER.info("KlientenHandler gestoppt.");
    this.serviceControl.stopAllServices();

    LOGGER.info("Sockets werden geschlossen.");
    this.serverDataModel.getServerConnectionDataManager().closeAllConnections();

    if (this.serverDataModel.getServerConnectionDataManager().noLiveServers()) {
      this.serverPersistentDataManager.getClientStateAccessManager().removeAllClientStates();
      LOGGER.info("Klientenzustaende entfernt. Dies ist der letzte Server.");
    }
    LOGGER.info("Server wurde heruntergefahren.");
  }

  /**
   * Setup server socket.
   *
   * @return the server socket
   */
  private LocalServerConnectionData setupServerSocket() {
    LocalServerConnectionData localServerConnectionData = null;
    final var serverPorts = ConnectionSpecifications.getServerports();
    final var hostname = ConnectionSpecifications.getHostname();

    for (var currentPortNumber : serverPorts) {

      if (!serverListening(hostname, currentPortNumber)) {

        try {
          ServerSocket masterSocket = new ServerSocket(currentPortNumber);
          LOGGER.info("Server wird auf Port {} Anfragen entgegen nehmen", currentPortNumber);
          localServerConnectionData = LocalServerConnectionData.valueOf(masterSocket.getLocalPort(),
              masterSocket);
          break;
        } catch (final IOException ioe) {
          LOGGER.error("ServerSocket konnte nicht auf Port {} geoeffnet werden. " + "{}: {}",
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
    this.serverPersistentDataManager = new ServerPersistentDataManager();
    this.serverPersistentDataManager.initiatePersistentAccess();
  }

  private void setupStaticServerDataAccess() {
    final var serverConnectionDataManager = this.serverDataModel.getServerConnectionDataManager();
    PacketCompiler.addOriginatorEntityProvider(
        () -> getServerEntity(
            serverConnectionDataManager.getLocalServerConnectionData().getServerId()));
    PacketCompiler.addContentIdentificator(new ServerContentIdentificationProviderImpl());
    HandlerAccessManager.setupStaticAccess(this.serverDataModel, this.serverPersistentDataManager);
    ClientSubscriptionHandler.setupStaticServerDataAccess();
    ClientStatePublisher.setupStaticServerDataAccess(serverConnectionDataManager);
    ClientStateRecorder.setupStaticServerDataAccess(
        this.serverPersistentDataManager.getClientStateAccessManager(),
        serverConnectionDataManager.getLocalServerConnectionData());
  }

  private void setupAndStartServices() {
    final TimerTask serviceMonitoring;
    this.serviceControl = new ServiceControl(this.serverDataModel,
        this.serverPersistentDataManager);
    this.serviceControl.startServices();
    serviceMonitoring = new ServiceHealthMonitor(this, this.serviceControl);
    this.serviceMonitor.scheduleAtFixedRate(serviceMonitoring, 200, 500);

    /* Bereits registrierte Nutzer werden registriert */
    waitForPrecedingServerConnections();
    loadRemoteClientConnections();
    /*
     * Server Synchronität wird eingetragen. Auswirkung auf
     * InterServerCommunicationService
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
      final var errorMessage = "Ungueltige Kombination von Host/Port: " + host + "/" + port + "\n";
      throw new IllegalArgumentException(errorMessage, uhe);
    } catch (final IOException ioe) {
      LOGGER.info("Kombination Host/Port: {}/{} frei.", host, port);
      return !listening;
    }
  }

  private void waitForPrecedingServerConnections() {
    final var connectionManager = this.serverDataModel.getServerConnectionDataManager();

    if (!connectionManager.remoteConnectionsLive()) {
      final var waitingStart = System.nanoTime();
      final var maxWait = TimeUnit.SECONDS.convert(10, TimeUnit.NANOSECONDS);

      do {
        if (waitingStart + maxWait > System.nanoTime()) {
          LOGGER.info("Warte auf Serververbindungen.");
          Thread.yield();
        } else {
          LOGGER.error("Verbindungsaufnahme zu aktiven Servern dauerte länger als {} Sekunden.",
              TimeUnit.SECONDS);
          this.shutdownServer();
        }
      } while (!connectionManager.remoteConnectionsLive());
      LOGGER.info("Entfernte Verbindungen aufgebaut.");
    }else{
      LOGGER.info("Keine entfernten Verbindungen abzuwarten.");
    }
  }

  /**
   * Laedt alle existiernden Klientenzustände. Prüft je entferntem Server-Buffer, ob entfernte
   * Klienten vorliegen. Dann werden, nach Zustand, Abonnements vorgenommen, wobei der überprüfte
   * InterServerbuffer hinterlegt wird. Dasselbe passiert für alle zusätzlichen Abonnements.
   */
  private void loadRemoteClientConnections() {
    LOGGER.info("Laden von entfernten Klienten.");

    final var clientSubscriptions = this.serverDataModel.getClientCategorySubscriptionManager();
    final var remoteSynchronizedServerIds = this.serverDataModel.getServerConnectionDataManager()
        .getAllSynchronizedRemoteServers();
    final var activeClients = this.serverPersistentDataManager.getClientStateAccessManager()
        .getAllActiveClientStates();

    if (!activeClients.isEmpty()) {

      for (final var remoteServerData : remoteSynchronizedServerIds) {
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
    this.serverDataModel.getServerConnectionDataManager().endPendingState();
    LOGGER.info("Laden von entfernten Klienten abgeschlossen.");
  }

  @Override
  public void serveClient(Socket clientConnectionSocket) {

    if (clientConnectionSocket != null) {
      final var requestAssignmentBuffer = this.serverDataModel.getServicePacketBufferManager()
          .getRandomBuffer(Service.TYPE.REQUEST_ROUTER);
      this.connectionPool.execute(
          new ClientConnectionHandler(clientConnectionSocket, requestAssignmentBuffer));
    } else {
      LOGGER.error("null-Socket kann nicht bedient werden.");
    }
  }
}
