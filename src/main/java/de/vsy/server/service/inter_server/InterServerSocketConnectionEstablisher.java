package de.vsy.server.service.inter_server;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

import de.vsy.server.server.data.ConnectionSpecifications;
import de.vsy.server.server.server_connection.LocalServerConnectionData;
import de.vsy.server.server.server_connection.RemoteServerConnectionData;
import de.vsy.server.server.server_connection.ServerConnectionDataManager;
import de.vsy.server.service.ServiceControl;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class InterServerSocketConnectionEstablisher implements
    InterServerCommunicationServiceCreator {

  private static final Logger LOGGER = LogManager.getLogger();
  private final ExecutorService establishingThread;
  private final ServiceControl serviceControl;
  private final ServerConnectionDataManager serverConnectionDataManager;
  private ServerSocket localMasterSocket;

  public InterServerSocketConnectionEstablisher(
      final ServerConnectionDataManager serverConnectionDataManager,
      final ServiceControl serviceControl) {
    this.serverConnectionDataManager = serverConnectionDataManager;
    this.serviceControl = serviceControl;
    this.establishingThread = newSingleThreadExecutor();
  }

  public void establishConnections() {
    setupServerSocket();
    connectToAllOperableServers();

    startFollowerConnections();
  }

  private void setupServerSocket() {
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
  }

  private void connectToAllOperableServers() {
    final var interServerPort = ConnectionSpecifications.getTransserverport();
    final var hostname = ConnectionSpecifications.getHostname();
    final var localMasterPort = this.localMasterSocket.getLocalPort();

    for (int test = 1, maxRunningServers = ConnectionSpecifications.getServerports()
        .size(); test <= maxRunningServers; test++) {
      final var testPort = interServerPort + test;

      if (testPort != localMasterPort) {

        try {
          final var s = new Socket(hostname, testPort);
          createInterServerService(false, s);
        } catch (IOException e) {
          LOGGER.error(
              "Remote connection to {}:{} failed",
              hostname, testPort);
        }
      }
    }
  }

  private void startFollowerConnections() {
    final var followerConnectionEstablisher = new ServerFollowerConnectionEstablisher(
        this.serverConnectionDataManager, this);
    this.establishingThread.submit(followerConnectionEstablisher);
  }

  @Override
  public void createInterServerService(final boolean isLeader,
      final Socket remoteServerConnection) {
    this.serverConnectionDataManager.addNotSynchronizedConnectionData(RemoteServerConnectionData
        .valueOf(remoteServerConnection.getLocalPort(), isLeader, remoteServerConnection));
    this.serviceControl.startInterServerCommThread();
  }

  public void stopEstablishingConnections() {
    this.establishingThread.shutdownNow();
    try{
      this.establishingThread.awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    LOGGER.info("FollowerAcceptor Thread terminated.");
  }

  public boolean isEstablishingConnections() {
    return !this.establishingThread.isTerminated() && !this.establishingThread.isShutdown();
  }
}
