package de.vsy.server.server.server_connection;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

import de.vsy.server.server.ClientServer;
import java.net.Socket;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClientConnectionEstablisher {

  private static final Logger LOGGER = LogManager.getLogger();
  private final ExecutorService clientConnectionPool;
  private final ClientServer server;
  private final LocalServerConnectionData clientConnectionData;

  public ClientConnectionEstablisher(final LocalServerConnectionData localClientConnectionData,
      final ClientServer server) {

    this.clientConnectionPool = newSingleThreadExecutor();
    this.clientConnectionData = localClientConnectionData;
    this.server = server;
  }

  public void startAcceptingClientConnetions() {
    final var clientConnectionAcceptor = clientConnectionData.getConnectionSocket();
    Future<Socket> newClientConnection;
    Socket clientConnectionSocket;

    if (clientConnectionAcceptor != null) {

      while (this.server.isOperable()) {
        newClientConnection = this.clientConnectionPool.submit(clientConnectionAcceptor::accept);

        try {
          clientConnectionSocket = newClientConnection.get();

          if (clientConnectionSocket != null) {
            this.server.serveClient(clientConnectionSocket);
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          LOGGER.error("Beim Warten auf Klientenverbindung unterbrochen.");
        } catch (ExecutionException e) {
          Thread.currentThread().interrupt();
          LOGGER.error(
              "Fehler bei der Verbindungsaufnahme von Klientenverbindungen. "
                  + "Fehlernachricht:\n{}",
              e.getMessage());
        }
      }
      LOGGER.info("Der Server wird heruntergefahren. Es werden keine "
          + "weiteren Klientenanfragen angenommen.");
    } else {
      LOGGER.error("Es wurde kein ServerSocket zur Verbindungsaufnahme " + "bereitgestellt.");
    }
  }

  public void stopAcceptingClientConnections() {
    this.clientConnectionPool.shutdownNow();

    do {
      LOGGER.info("KlientenAcceptor-Thread Ende wird erwartet.");
      Thread.yield();
    } while (!this.clientConnectionPool.isTerminated());
    LOGGER.info("KlientenAcceptor-Thread gestoppt.");
  }
}
