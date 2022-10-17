package de.vsy.server.service.inter_server;

import static de.vsy.shared_utility.standard_value.ThreadContextValues.LOG_FILE_CONTEXT_KEY;
import static de.vsy.shared_utility.standard_value.ThreadContextValues.LOG_ROUTE_CONTEXT_KEY;
import static de.vsy.shared_utility.standard_value.ThreadContextValues.STANDARD_SERVER_ROUTE_VALUE;

import de.vsy.server.persistent_data.server_data.temporal.LiveClientStateDAO;
import de.vsy.shared_utility.logging.ThreadContextTimerTask;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.ThreadContext;

public class ClientReconnectionStateWatcher extends ThreadContextTimerTask {

  private final LiveClientStateDAO clientStateAccessor;
  private final List<Integer> pendingClientIdList;
  private final ClientReconnectionHandler reconnectionHandler;

  public ClientReconnectionStateWatcher(final LiveClientStateDAO clientStateAccess,
      final List<Integer> pendingClientIdList,
      final ClientReconnectionHandler reconnectionHandler) {
    this.clientStateAccessor = clientStateAccess;
    this.pendingClientIdList = pendingClientIdList;
    this.reconnectionHandler = reconnectionHandler;
  }

  @Override
  public void runWithContext() {
    ThreadContext.put(LOG_ROUTE_CONTEXT_KEY, STANDARD_SERVER_ROUTE_VALUE);
    ThreadContext.put(LOG_FILE_CONTEXT_KEY, "reconnectWatcher");

    List<Integer> reconnectedClientIds = new ArrayList<>();

    for (final var currentClientId : pendingClientIdList) {

      if (this.clientStateAccessor.getClientReconnectionState(currentClientId)) {
        reconnectionHandler.processReconnection(currentClientId);
        reconnectedClientIds.add(currentClientId);
      } else {
        if (this.clientStateAccessor.getClientPendingState(currentClientId)) {
          reconnectedClientIds.add(currentClientId);
        }
      }
    }
    this.pendingClientIdList.removeAll(reconnectedClientIds);
  }
}
