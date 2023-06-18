package de.vsy.server.service.inter_server;

import de.vsy.server.data.SocketConnectionDataManager;
import de.vsy.server.data.socketConnection.RemoteServerConnectionData;
import de.vsy.shared_utility.logging.ThreadContextRunnable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import static de.vsy.server.data.socketConnection.SocketConnectionState.UNINITIATED;

public class ServerFollowerConnectionEstablisher extends ThreadContextRunnable {

    private static final Logger LOGGER = LogManager.getLogger();
    private final SocketConnectionDataManager serverConnectionManager;
    private final InterServerCommunicationServiceCreator serviceCreator;

    public ServerFollowerConnectionEstablisher(
            final SocketConnectionDataManager serverConnectionManager,
            final InterServerCommunicationServiceCreator serviceCreator) {
        this.serviceCreator = serviceCreator;
        this.serverConnectionManager = serverConnectionManager;
    }

    @Override
    public void runWithContext() {
        Thread.currentThread().setName("ServerFollowerConnectionEstablisher");
        LOGGER.info("{} started.", Thread.currentThread().getName());
        final var watchedSocket = this.serverConnectionManager.getServerReceptionConnectionData()
                .getConnectionSocket();

        while (!(Thread.interrupted()) && !watchedSocket.isClosed()) {
            final var followerSocket = acceptFollowerConnection(watchedSocket);

            if (followerSocket != null && !followerSocket.isClosed()) {
                final var remoteServerConnection = RemoteServerConnectionData.valueOf(
                        followerSocket.getLocalPort(), true, followerSocket);
                this.serverConnectionManager.addServerConnection(UNINITIATED, remoteServerConnection);
                this.serviceCreator.startInterServerCommThread();
            }
        }
        LOGGER.info("{} stopped. Socket closed: {}",
                Thread.currentThread().getName(), watchedSocket.isClosed());
    }

    /**
     * Let's single ExecutorService thread wait for new server connection and returns new connection
     * socket. InterruptedExceptions are logged and interrupt flag is set. IOExceptions are expected
     * and logged, but no further action is taken.
     *
     * @param socketToWatch the server socket that waits for new connection.
     * @return new Socket or null if handled exception occurred.
     */
    public Socket acceptFollowerConnection(final ServerSocket socketToWatch) {

        try {
            return socketToWatch.accept();
        } catch (IOException e) {
            LOGGER.error("{} occurred while waiting for server connection.",
                    e.getClass().getSimpleName());
            return null;
        }
    }
}
