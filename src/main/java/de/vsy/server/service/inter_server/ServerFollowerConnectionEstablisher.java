package de.vsy.server.service.inter_server;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

import de.vsy.server.server.server_connection.ServerConnectionDataManager;
import de.vsy.shared_utility.logging.ThreadContextRunnable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServerFollowerConnectionEstablisher extends ThreadContextRunnable {

  private static final Logger LOGGER = LogManager.getLogger();
  private final ExecutorService acceptingThread;
  private final ServerConnectionDataManager serverConnectionManager;
  private final InterServerCommunicationServiceCreator serviceCreator;

  public ServerFollowerConnectionEstablisher(
      final ServerConnectionDataManager serverConnectionManager,
      final InterServerCommunicationServiceCreator serviceCreator) {

    this.acceptingThread = newSingleThreadExecutor();
    this.serviceCreator = serviceCreator;
    this.serverConnectionManager = serverConnectionManager;
  }

  @Override
  public void runWithContext() {
    Thread.currentThread().setName("ServerFollowerConnectionEstablisher");
    LOGGER.info("{} started.", Thread.currentThread().getName());
    final var watchedSocket = this.serverConnectionManager.getServerReceptionConnectionData()
        .getConnectionSocket();

    while (!Thread.currentThread().isInterrupted() && !watchedSocket.isClosed()) {
      final var followerSocket = acceptFollowerConnection(watchedSocket);

      if (followerSocket != null && !followerSocket.isClosed()) {
        this.serviceCreator.createInterServerService(true, followerSocket);
      }
    }
    LOGGER.info("{} stopped. Thread interrupted: {} / socket closed: {}",
        Thread.currentThread().getName(), Thread.currentThread().isInterrupted(),
        watchedSocket.isClosed());
  }

  /**
   * Let's single ExecutorService thread wait for new server connection and returns new connection
   * socket. InterruptedExceptions are logged and interrupt flag is set. IOExceptions are expected
   * and logged, but no further action is taken.
   *
   * @param socketToWatch the server socket that waits for new connection.
   * @return new Socket or null if handled exception occurred.
   * @throws RuntimeException rethrow causes: ServerSocket -> SecurityException,
   *                          SocketTimeoutException, IllegalBlockingModeException; Future ->
   *                          CancellationException
   */
  public Socket acceptFollowerConnection(final ServerSocket socketToWatch) {
    Socket followerSocket = null;

    try {
      final var futureFollower = this.acceptingThread.submit(socketToWatch::accept);
      followerSocket = futureFollower.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      LOGGER.error(e.getClass().getSimpleName(), e.getCause());
    } catch (ExecutionException ee) {
      var cause = ee.getCause();

      if (cause instanceof IOException) {
        LOGGER.error(ee.getMessage(), cause);
      } else {
        LOGGER.error("Exception occurred while getting new remote server socket from Future.");
        throw new RuntimeException(ee);
      }
    }
    return followerSocket;
  }
}
