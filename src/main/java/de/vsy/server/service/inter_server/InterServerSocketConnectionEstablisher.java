package de.vsy.server.service.inter_server;

import static de.vsy.server.server.data.socketConnection.SocketConnectionState.UNINITIATED;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

import de.vsy.server.server.data.ConnectionSpecifications;
import de.vsy.server.server.data.ServerSynchronizationManager;
import de.vsy.server.server.data.socketConnection.LocalServerConnectionData;
import de.vsy.server.server.data.socketConnection.RemoteServerConnectionData;
import de.vsy.server.server.data.SocketConnectionDataManager;
import de.vsy.server.service.ServiceControl;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class InterServerSocketConnectionEstablisher {

  private static final Logger LOGGER = LogManager.getLogger();
  private final ServerSynchronizationManager serverSynchronization;
  private final ExecutorService establishingThread;
  private final ServiceControl serviceControl;
  private final SocketConnectionDataManager serverConnectionDataManager;
  private ServerSocket localMasterSocket;

  public InterServerSocketConnectionEstablisher(
      final ServerSynchronizationManager serverSynchronization,
      final SocketConnectionDataManager serverConnectionDataManager,
      final ServiceControl serviceControl) {
    this.serverSynchronization = serverSynchronization;
    this.serverConnectionDataManager = serverConnectionDataManager;
    this.serviceControl = serviceControl;
    this.establishingThread = newSingleThreadExecutor();
  }

  /**
   * ChatServer laesst InterServerSocketConnectionEstablisher laden → Liveserver werden synchron
   * geladen, Verbindung aber asynchron initialisiert und synchronisiert =>
   * InterServerCommunicationService wartet nach Initialisierung auf Laden entfernter Kontakte →
   * ChatServer wartet auf Initialisierung von entfernten Verbindungen ChatServer laedt entfernt
   * verbundene Kontakte
   * <p>
   * - Problem: Liveserversockets werden synchron hinzugefuegt, aber asynchron entfernt → in
   * separates Set im connectToAllOperableServers hinzufuegen und dann pruefen → wo?
   */
  public void establishConnections() {
    setupServerSocket();
    connectToAllOperableServers();
    startFollowerConnections();
  }

  private void setupServerSocket() {
    LOGGER.info("Trying to setup ServerSocket for inter server connection purposes.");
    LocalServerConnectionData serverReceptionConnection;
    var masterSocketPort = ConnectionSpecifications.getTransserverport();

    do {
      masterSocketPort++;
      try {
        this.localMasterSocket = new ServerSocket(masterSocketPort);
      } catch (IOException e) {
        LOGGER.error("Inter ServerSocket could not be created on Port {}.",
            masterSocketPort);
      }
    } while (this.localMasterSocket == null);
    serverReceptionConnection = LocalServerConnectionData.valueOf(masterSocketPort,
        this.localMasterSocket);
    this.serverConnectionDataManager.addServerReceptionConnectionData(serverReceptionConnection);
    LOGGER.info("Done setting up ServerSocket for inter server connection purposes.");
  }

  private void connectToAllOperableServers() {
    LOGGER.info("Trying to establish connections with preexisting chat servers.");
    final var interServerPort = ConnectionSpecifications.getTransserverport();
    final var hostname = ConnectionSpecifications.getHostname();
    final var localMasterPort = this.localMasterSocket.getLocalPort();

    for (int test = 1, maxRunningServers = ConnectionSpecifications.getServerports()
        .size(); test <= maxRunningServers; test++) {
      final var testPort = interServerPort + test;

      if (testPort != localMasterPort) {

        try {
          final var remoteServer = new Socket(hostname, testPort);
          this.serverConnectionDataManager.addServerConnection(UNINITIATED,
              RemoteServerConnectionData
                  .valueOf(remoteServer.getLocalPort(), false, remoteServer));
          LOGGER.info("Remote connection established: {}:{}.", hostname, testPort);
        } catch (IOException e) {
          LOGGER.warn(
              "Remote connection to {}:{} failed",
              hostname, testPort);
        }
      }
      LOGGER.info("Finished establishing connections with preexisting chat servers.");
    }
  }

  private void startFollowerConnections() {
    LOGGER.info("ServerConnectionEstablisher thread initiated.");
    final var followerConnectionEstablisher = new ServerFollowerConnectionEstablisher(
        this.serverConnectionDataManager, serviceControl);
    this.establishingThread.submit(followerConnectionEstablisher);
  }

  public void stopEstablishingConnections() {
    LOGGER.info("ServerConnectionEstablisher thread termination initiated.");
    this.establishingThread.shutdownNow();
    try {
      this.establishingThread.awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    LOGGER.info("ServerConnectionEstablisher thread terminated.");
  }

  public boolean isEstablishingConnections() {
    return !this.establishingThread.isTerminated() && !this.establishingThread.isShutdown();
  }
}
