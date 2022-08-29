package de.vsy.chat.server.service.inter_server;

import de.vsy.chat.server.persistent_data.server_data.temporal.LiveClientStateDAO;
import de.vsy.chat.shared_utility.logging.ThreadContextTimerTask;
import org.apache.logging.log4j.ThreadContext;

import java.util.ArrayList;
import java.util.List;

public
class ClientReconnectionStateWatcher extends ThreadContextTimerTask {

    private final LiveClientStateDAO clientStateAccessor;
    private final List<Integer> pendingClientIdList;
    private final ClientReconnectionHandler reconnectionHandler;

    public
    ClientReconnectionStateWatcher (final LiveClientStateDAO clientStateAccess,
                                    final List<Integer> pendingClientIdList,
                                    final ClientReconnectionHandler reconnectionHandler) {
        this.clientStateAccessor = clientStateAccess;
        this.pendingClientIdList = pendingClientIdList;
        this.reconnectionHandler = reconnectionHandler;
    }

    @Override
    public
    void runWithContext () {
        ThreadContext.put("routeDir", "serverLog");
        ThreadContext.put("logFilename", "reconnectWatcher");

        List<Integer> reconnectedClientIds = new ArrayList<>();

        for (final var currentClientId : pendingClientIdList) {

            if (this.clientStateAccessor.getClientReconnectionState(
                    currentClientId)) {
                reconnectionHandler.processReconnection(currentClientId);
                reconnectedClientIds.add(currentClientId);
            } else if (this.clientStateAccessor.getClientPendingState(
                    currentClientId)) {
                reconnectedClientIds.add(currentClientId);
            }
        }
        this.pendingClientIdList.removeAll(reconnectedClientIds);
    }
}
