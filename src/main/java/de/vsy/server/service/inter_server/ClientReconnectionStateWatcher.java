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
                                          final List<Integer> pendingClientIds,
                                          final ClientReconnectionHandler reconnectionHandler) {
        this.clientStateAccessor = clientStateAccess;
        this.reconnectionHandler = reconnectionHandler;

        if (pendingClientIds == null || pendingClientIds.isEmpty()) {
            reconnectionHandler.stopReconnectingClients();
        } else {
            this.pendingClientIds = new LinkedList<>(pendingClientIds);
        }
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

        if (stillPendingClientIds.isEmpty()) {
            this.reconnectionHandler.stopReconnectingClients();
        } else {
            this.pendingClientIds = stillPendingClientIds;
        }
    }
}
