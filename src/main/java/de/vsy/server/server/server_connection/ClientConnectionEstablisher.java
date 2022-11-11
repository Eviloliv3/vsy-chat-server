package de.vsy.server.server.server_connection;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

import de.vsy.server.server.ClientServer;
import de.vsy.server.server.data.socketConnection.LocalServerConnectionData;
import java.net.Socket;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClientConnectionEstablisher {

  private static final Logger LOGGER = LogManager.getLogger();
  private final AtomicBoolean serverHealthFlag;
  private final ExecutorService clientConnectionAcceptor;
  private final ClientServer server;
  private final LocalServerConnectionData clientConnectionData;

  public ClientConnectionEstablisher(final LocalServerConnectionData localClientConnectionData,
      final ClientServer server) {
    this.serverHealthFlag = new AtomicBoolean(true);
    this.clientConnectionAcceptor = newSingleThreadExecutor();
    this.clientConnectionData = localClientConnectionData;
    this.server = server;
  }

  public void acceptClientConnections() {
    final var clientConnectionAcceptor = clientConnectionData.getConnectionSocket();
    Socket clientConnectionSocket;

    if (clientConnectionAcceptor != null) {

      while (serverHealthFlag.get()) {
        var newClientConnection = this.clientConnectionAcceptor.submit(
            clientConnectionAcceptor::accept);

        try {
          clientConnectionSocket = newClientConnection.get();

          if (clientConnectionSocket != null) {
            this.server.serveClient(clientConnectionSocket);
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          LOGGER.error("Interrupted while waiting for a new client connection.");
          break;
        } catch (ExecutionException e) {
          Thread.currentThread().interrupt();
          LOGGER.error(
              "Error occurred while waiting for a new client connection. "
                  + "Error message:\n{}",
              e.getMessage());
          break;
        }
      }
      LOGGER.info("No new client connections will be accepted.");
    } else {
      LOGGER.error("No ServerSocket has been provided.");
    }
  }

  public void changeServerHealthFlag(final boolean newHealthState) {
    this.serverHealthFlag.set(newHealthState);
  }

  public void stopEstablishingConnections() throws InterruptedException {
    this.clientConnectionAcceptor.shutdownNow();
    LOGGER.info("Client connection thread termination initiated.");
    this.clientConnectionAcceptor.awaitTermination(500, TimeUnit.MILLISECONDS);
    LOGGER.info("Client connection thread terminated.");
  }
}
