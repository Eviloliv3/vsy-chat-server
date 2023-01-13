package de.vsy.server.client_handling.data_management;

import de.vsy.server.client_handling.data_management.logic.ClientSubscriptionHandler;
import de.vsy.server.client_handling.packet_processing.request_filter.PermittedPacketCategoryCheck;
import de.vsy.server.client_handling.persistent_data_access.ClientPersistentDataAccessProvider;

public class LocalClientStateObserverManager {

    private final PermittedPacketCategoryCheck permittedPackets;
    private final ClientPersistentDataAccessProvider clientPersistentAccess;
    private final ClientSubscriptionHandler clientSubscriptionHandler;

    public LocalClientStateObserverManager(final HandlerLocalDataManager handlerDataAccess) {
        this.permittedPackets = new PermittedPacketCategoryCheck();
        this.clientPersistentAccess = new ClientPersistentDataAccessProvider(
                handlerDataAccess.getLocalClientDataProvider());
        this.clientSubscriptionHandler = new ClientSubscriptionHandler(
                handlerDataAccess.getLocalClientDataProvider(),
                handlerDataAccess.getHandlerBufferManager(),
                this.clientPersistentAccess.getContactListDAO());
        addStateListeners(handlerDataAccess);
    }

    private void addStateListeners(final HandlerLocalDataManager handlerDataAccess) {
        final var stateManager = handlerDataAccess.getClientStateManager();

        stateManager.addStateListener(this.permittedPackets);
        stateManager.addStateListener(this.clientPersistentAccess);
        stateManager.addStateListener(this.clientSubscriptionHandler);
    }

    public PermittedPacketCategoryCheck getPermittedPacketCategoryCheck() {
        return permittedPackets;
    }

    public ClientPersistentDataAccessProvider getClientPersistentAccess() {
        return clientPersistentAccess;
    }

    public ClientSubscriptionHandler getClientSubscriptionHandler() {
        return this.clientSubscriptionHandler;
    }
}
