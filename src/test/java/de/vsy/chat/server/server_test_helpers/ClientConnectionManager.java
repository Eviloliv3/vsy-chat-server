package de.vsy.chat.server.server_test_helpers;

import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_CLIENT_ID;

import de.vsy.chat.server.raw_server_test.ServerPortProvider;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClientConnectionManager {

  protected static final Logger LOGGER = LogManager.getLogger();
  protected final List<ClientConnection> clientConnectionList;
  protected final ServerPortProvider serverPorts;

  public ClientConnectionManager(final ServerPortProvider serverPorts) {
    this.serverPorts = serverPorts;
    this.clientConnectionList = new ArrayList<>();
  }

  public ClientConnection getConnection(int clientNumber) {
    ClientConnection foundClient = null;
    final var validIndex = (clientNumber < this.clientConnectionList.size()) && (clientNumber >= 0);

    if (validIndex) {
      foundClient = clientConnectionList.get(clientNumber);
    }
    return foundClient;
  }

  public ClientConnection getUnusedConnection() {
    ClientConnection foundConnection = null;

    for (final var currentConnection : this.clientConnectionList) {
      if (currentConnection.getCommunicatorData().getCommunicatorId() == STANDARD_CLIENT_ID) {
        foundConnection = currentConnection;
        break;
      }
    }
    return foundConnection;
  }

  public int getClientConnectionCount() {
    return this.clientConnectionList.size();
  }
}
