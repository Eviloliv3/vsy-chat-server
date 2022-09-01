/*
 *
 */
package de.vsy.server.server.data.access;

import de.vsy.server.persistent_data.server_data.ClientTransactionDAO;
import de.vsy.server.persistent_data.server_data.temporal.LiveClientStateDAO;
import de.vsy.server.server.data.AbstractPacketCategorySubscriptionManager;
import de.vsy.server.server.data.ServerDataManager;
import de.vsy.server.server.data.ServerPersistentDataManager;
import de.vsy.server.server.server_connection.LocalServerConnectionData;

/**
 * Provides limited accessLimiter to server managed buffers and information
 * concerning other connected clients. Zugriffsfassade für KlientenProcessor auf
 * servereigene Daten.
 */
public
class HandlerAccessManager {

    private static LiveClientStateDAO clientStatePersistenceManager;
    private static AbstractPacketCategorySubscriptionManager clientSubscriptionHandler;
    private static ClientTransactionDAO clientTransactionManager;
    private static CommunicatorDataManipulator communicatorDataManipulator;
    private static LocalServerConnectionData serverNodeData;

    private
    HandlerAccessManager () {
    }

    public static
    void setupStaticAccess (final ServerDataManager serverDataAccess,
                            final ServerPersistentDataManager serverPersistentDataManager) {
        clientStatePersistenceManager = serverPersistentDataManager.getClientStateAccessManager();
        clientSubscriptionHandler = serverDataAccess.getClientCategorySubscriptionManager();
        clientTransactionManager = serverPersistentDataManager.getTransactionAccessManager();
        communicatorDataManipulator = new CommunicatorDataManipulator(
                serverPersistentDataManager);
        serverNodeData = serverDataAccess.getServerConnectionDataManager()
                                         .getLocalServerConnectionData();
    }

    public static
    LocalServerConnectionData getLocalServerConnectionData () {
        return serverNodeData;
    }

    public static
    ClientTransactionDAO getClientTransactionAccessManager () {
        return clientTransactionManager;
    }

    public static
    CommunicatorDataManipulator getCommunicatorDataManipulator () {
        return communicatorDataManipulator;
    }

    public static
    AbstractPacketCategorySubscriptionManager getClientSubscriptionManager () {
        return clientSubscriptionHandler;
    }

    public static
    LiveClientStateDAO getClientStateAccessManager () {
        return clientStatePersistenceManager;
    }
}