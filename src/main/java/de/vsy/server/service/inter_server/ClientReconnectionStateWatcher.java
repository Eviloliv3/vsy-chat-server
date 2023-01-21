package de.vsy.server.service.inter_server;

import de.vsy.server.persistent_data.server_data.temporal.LiveClientStateDAO;
import de.vsy.shared_utility.logging.ThreadContextTimerTask;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


public class ClientReconnectionStateWatcher extends ThreadContextTimerTask {

    private final LiveClientStateDAO clientStateAccessor;
    private final ClientReconnectionHandler reconnectionHandler;
    private Queue<Integer> pendingClientIds;

    public ClientReconnectionStateWatcher(final LiveClientStateDAO clientStateAccess,
                                          final List<Integer> pendingClientIdList,
                                          final ClientReconnectionHandler reconnectionHandler) {
        this.clientStateAccessor = clientStateAccess;
        this.reconnectionHandler = reconnectionHandler;
        this.pendingClientIds = new LinkedList<>(pendingClientIdList);
    }

    @Override
    public void runWithContext() {
        final Queue<Integer> stillPendingClientIds = new LinkedList<>();

        do {
            final var currentClientId = pendingClientIds.poll();

            if (this.clientStateAccessor.getClientReconnectionState(currentClientId)) {
                this.reconnectionHandler.processReconnection(currentClientId);
            } else {
                stillPendingClientIds.add(currentClientId);
            }
        } while (!(this.pendingClientIds.isEmpty()));
        this.pendingClientIds = stillPendingClientIds;
    }
}
