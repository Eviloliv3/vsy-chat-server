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
    LOGGER.info("{} gestartet.", Thread.currentThread().getName());
    final var watchedSocket = this.serverConnectionManager.getServerReceptionConnectionData()
        .getConnectionSocket();

    while (!Thread.currentThread().isInterrupted() && !watchedSocket.isClosed()) {
      final var followerSocket = acceptFollowerConnection(watchedSocket);

      if (followerSocket != null) {
        this.serviceCreator.createInterServerService(true, followerSocket);
      }
      Thread.yield();
    }
    LOGGER.info("{} gestoppt.", Thread.currentThread().getName());
  }

  public Socket acceptFollowerConnection(final ServerSocket socketToWatch) {
    Socket followerSocket = null;

    try (socketToWatch) {
      final var futureFollower = this.acceptingThread.submit(socketToWatch::accept);
      followerSocket = futureFollower.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      LOGGER.info("Verbindungsaufnahme mit Follower-Server wurde " + "unterbrochen.");
    } catch (IOException ioe) {
      Thread.currentThread().interrupt();
      LOGGER.error("Fehler bei der Verbindungsaufnahme mit Follower-Servern. Fehlernachricht:\n{}",
          ioe.getMessage());
    } catch (ExecutionException ee) {
      Thread.currentThread().interrupt();
      LOGGER.error("Fehler beim Holen des Sockets vom Future.");
    }
    return followerSocket;
  }
}
