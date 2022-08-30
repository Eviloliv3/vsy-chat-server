package de.vsy.server.client_handling.data_management;

import de.vsy.server.client_handling.data_management.logic.ClientStatePublisher;
import de.vsy.server.client_handling.data_management.logic.ClientSubscriptionHandler;
import de.vsy.shared_module.shared_module.packet_management.ThreadPacketBufferLabel;
import de.vsy.server.client_handling.packet_processing.request_filter.PermittedPacketCategoryCheck;
import de.vsy.server.client_handling.persistent_data_access.ClientPersistentDataAccessProvider;

public
class LocalClientStateDependentLogicProvider {

    private final PermittedPacketCategoryCheck permittedPackets;
    private final ClientPersistentDataAccessProvider clientPersistentAccess;
    private final ExtraClientSubscriptionProvider extraSubscriptionProvider;
    private final ClientSubscriptionHandler clientSubscriptionHandler;
    private final ClientStatePublisher clientStatePublisher;

    public
    LocalClientStateDependentLogicProvider (
            final HandlerLocalDataManager handlerDataAccess) {
        this.permittedPackets = new PermittedPacketCategoryCheck();
        this.clientPersistentAccess = new ClientPersistentDataAccessProvider(
                handlerDataAccess.getLocalClientDataProvider());
        this.extraSubscriptionProvider = new ExtraClientSubscriptionProvider(
                this.clientPersistentAccess.getContactlistDAO());
        this.clientSubscriptionHandler = new ClientSubscriptionHandler(
                extraSubscriptionProvider,
                handlerDataAccess.getLocalClientDataProvider(),
                handlerDataAccess.getHandlerBufferManager());
        this.clientStatePublisher = new ClientStatePublisher(
                handlerDataAccess.getLocalClientDataProvider(),
                this.clientPersistentAccess.getContactlistDAO(),
                handlerDataAccess.getHandlerBufferManager()
                                 .getPacketBuffer(
                                         ThreadPacketBufferLabel.SERVER_BOUND)::appendPacket);
        addStateListeners(handlerDataAccess);
    }

    private
    void addStateListeners (final HandlerLocalDataManager handlerDataAccess) {
        final var stateManager = handlerDataAccess.getClientStateManager();

        stateManager.addStateListener(this.permittedPackets);
        stateManager.addStateListener(this.clientPersistentAccess);
        stateManager.addStateListener(this.clientStatePublisher);
        stateManager.addStateListener(this.clientSubscriptionHandler);
    }

    public
    PermittedPacketCategoryCheck getPermittedPacketCategoryCheck () {
        return permittedPackets;
    }

    public
    ClientPersistentDataAccessProvider getClientPersistentAccess () {
        return clientPersistentAccess;
    }

    public
    ExtraClientSubscriptionProvider getExtraClientSubscriptionProvider () {
        return this.extraSubscriptionProvider;
    }

    public
    ClientSubscriptionHandler getClientSubscriptionHandler () {
        return this.clientSubscriptionHandler;
    }

    public
    ClientStatePublisher getClientStatePublisher () {
        return this.clientStatePublisher;
    }
}
