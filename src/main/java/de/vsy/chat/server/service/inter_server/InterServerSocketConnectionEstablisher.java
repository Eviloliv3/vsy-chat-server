package de.vsy.chat.server.service.inter_server;

import de.vsy.chat.server.server.data.ConnectionSpecifications;
import de.vsy.chat.server.server.server_connection.LocalServerConnectionData;
import de.vsy.chat.server.server.server_connection.RemoteServerConnectionData;
import de.vsy.chat.server.server.server_connection.ServerConnectionDataManager;
import de.vsy.chat.server.service.ServiceControl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

public
class InterServerSocketConnectionEstablisher
        implements InterServerCommunicationServiceCreator {

    private static final Logger LOGGER = LogManager.getLogger();
    private final ExecutorService establishingThread;
    private final ServiceControl serviceControl;
    private final ServerConnectionDataManager serverConnectionDataManager;
    private ServerSocket localMasterSocket;

    public
    InterServerSocketConnectionEstablisher (
            final ServerConnectionDataManager serverConnectionDataManager,
            final ServiceControl serviceControl) {
        this.serverConnectionDataManager = serverConnectionDataManager;
        this.serviceControl = serviceControl;
        this.establishingThread = newSingleThreadExecutor();
    }

    public
    void establishConnections () {
        setupServerSocket();
        connectToAllOperableServers();

        startFollowerConnections();
    }

    private
    void setupServerSocket () {
        LocalServerConnectionData serverReceptionConnection;
        var masterSocketPort = ConnectionSpecifications.getTransserverport();

        do {
            masterSocketPort++;
            try {
                this.localMasterSocket = new ServerSocket(masterSocketPort);
            } catch (IOException e) {
                LOGGER.error("InterServerSocket konnte nicht auf " +
                             "Port {} geoeffnet werden.", masterSocketPort);
            }
        } while (this.localMasterSocket == null);
        serverReceptionConnection = LocalServerConnectionData.valueOf(
                masterSocketPort, this.localMasterSocket);
        this.serverConnectionDataManager.addServerReceptionConnectionData(
                serverReceptionConnection);
    }

    private
    void connectToAllOperableServers () {
        final var interServerPort = ConnectionSpecifications.getTransserverport();
        final var hostname = ConnectionSpecifications.getHostname();
        final var localMasterPort = this.localMasterSocket.getLocalPort();

        for (int test = 1, maxRunningServers = ConnectionSpecifications.getServerports()
                                                                       .size();
             test <= maxRunningServers;
             test++) {
            final var testPort = interServerPort + test;

            if (testPort != localMasterPort) {

                try {
                    final var s = new Socket(hostname, testPort);
                    createInterServerService(false, s);
                } catch (IOException e) {
                    LOGGER.error("Es konnte keine Verbindung zu entferntem " +
                                 "Server aufgebaut werden. {}:{}", hostname,
                                 testPort);
                }
            }
        }
    }

    private
    void startFollowerConnections () {
        final var followerConnectionEstablisher = new ServerFollowerConnectionEstablisher(
                this.serverConnectionDataManager, this);
        this.establishingThread.submit(followerConnectionEstablisher);
    }

    @Override
    public
    void createInterServerService (final boolean isLeader,
                                   final Socket remoteServerConnection) {
        this.serverConnectionDataManager.addNotSynchronizedConnectionData(
                RemoteServerConnectionData.valueOf(
                        remoteServerConnection.getLocalPort(), isLeader,
                        remoteServerConnection));
        this.serviceControl.startInterServerCommThread();
    }

    public
    void stopEstabilishingConnections () {
        this.establishingThread.shutdownNow();

        do {
            LOGGER.info(
                    "Es wird noch auf den FollowerAcceptor-Thread " + " gewartet.");
            Thread.yield();
        } while (!this.establishingThread.isTerminated());
        LOGGER.info("FollowerAcceptor-Thread gestoppt.");
    }
}
