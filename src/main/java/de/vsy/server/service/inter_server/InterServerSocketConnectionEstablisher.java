package de.vsy.server.service.inter_server;

import de.vsy.server.data.ConnectionSpecifications;
import de.vsy.server.data.SocketConnectionDataManager;
import de.vsy.server.data.socketConnection.LocalServerConnectionData;
import de.vsy.server.data.socketConnection.RemoteServerConnectionData;
import de.vsy.server.service.ServiceControl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

import static de.vsy.server.data.socketConnection.SocketConnectionState.UNINITIATED;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;

public class InterServerSocketConnectionEstablisher {

    private static final Logger LOGGER = LogManager.getLogger();
    private final ExecutorService establishingThread;
    private final ServiceControl serviceControl;
    private final SocketConnectionDataManager serverConnectionDataManager;
    private ServerSocket localMasterSocket;

    public InterServerSocketConnectionEstablisher(final SocketConnectionDataManager serverConnectionDataManager,
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
        LOGGER.info("Trying to setup ServerSocket for inter server connection purposes.");
        LocalServerConnectionData serverReceptionConnection;
        var masterSocketPort = ConnectionSpecifications.getInterServerPort();

        do {
            try {
                this.localMasterSocket = new ServerSocket(masterSocketPort);
            } catch (IOException e) {
                LOGGER.error("InterServerSocket could not be created on Port {}.",
                        masterSocketPort);
            }
            masterSocketPort++;
        } while (this.localMasterSocket == null);
        serverReceptionConnection = LocalServerConnectionData.valueOf(masterSocketPort,
                this.localMasterSocket);
        this.serverConnectionDataManager.addServerReceptionConnectionData(serverReceptionConnection);
        LOGGER.info("Done setting up ServerSocket for inter server connection purposes.");
    }

    private void connectToAllOperableServers() {
        LOGGER.info("Trying to establish connections with preexisting chat servers.");
        final var interServerPort = ConnectionSpecifications.getInterServerPort();
        final var hostname = ConnectionSpecifications.getHostname();
        final var localMasterPort = this.localMasterSocket.getLocalPort();
        final var maxRunningServers = ConnectionSpecifications.getServerPorts().size();

        for (int test = 0; test < maxRunningServers; test++) {
            final var testPort = interServerPort + test;

            if (testPort == localMasterPort) {
                LOGGER.trace("Port {} will be ignored, as it is local ServerSocket port.", testPort);
                continue;
            }

            try {
                final var remoteServer = new Socket(hostname, testPort);
                final var remoteServerConnection = RemoteServerConnectionData.valueOf(remoteServer.getLocalPort(), false, remoteServer);
                this.serverConnectionDataManager.addServerConnection(UNINITIATED, remoteServerConnection);
                LOGGER.info("Remote connection established: {}:{}.", hostname, testPort);
            } catch (IOException e) {
                LOGGER.warn("Remote connection to {}:{} failed", hostname, testPort);
            }
        }
        LOGGER.info("Finished establishing connections with preexisting chat servers.");
    }

    private void startFollowerConnections() {
        LOGGER.info("ServerConnectionEstablisher thread initiated.");
        final var followerConnectionEstablisher = new ServerFollowerConnectionEstablisher(
                this.serverConnectionDataManager, serviceControl);
        this.establishingThread.submit(followerConnectionEstablisher);
    }

    public void stopEstablishingConnections() {
        LOGGER.info("ServerConnectionEstablisher thread termination initiated.");
        try {
            this.localMasterSocket.close();
        } catch (IOException e) {
            LOGGER.error("{} during ServerSocket closing attempt.", e.getClass().getSimpleName());
        }
        this.establishingThread.shutdownNow();

        try {
            final var interServerEstablisherDown = this.establishingThread.awaitTermination(5, SECONDS);

            if (interServerEstablisherDown) {
                LOGGER.info("ServerConnectionEstablisher thread terminated.");
            } else {
                LOGGER.error(
                        "ServerConnectionEstablisher shutdown unexpectedly took more than 5 seconds and may be deadlocked.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error(
                    "Interrupted while waiting for ServerConnectionEstablisher thread to terminate.");
        }
    }

    public boolean isEstablishingConnections() {
        return !this.establishingThread.isTerminated() && !this.establishingThread.isShutdown();
    }
}
