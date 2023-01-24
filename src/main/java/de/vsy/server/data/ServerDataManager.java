package de.vsy.server.data;

import de.vsy.server.data.socketConnection.LocalServerConnectionData;
import de.vsy.server.service.ServicePacketBufferManager;

/**
 * Manages all server specific data.
 */
public class ServerDataManager {

    private final ServerSynchronizationManager serverSynchronization;
    private final SocketConnectionDataManager serverNodeManager;
    private final PacketCategorySubscriptionManager clientSubscriptionManager;
    private final PacketCategorySubscriptionManager serviceSubscriptionManager;
    private final ServicePacketBufferManager servicePacketBufferManager;

    /**
     * Instantiates a new server dataManagement manager.
     */
    public ServerDataManager(final LocalServerConnectionData localServerConnectionData) {
        this.clientSubscriptionManager = new ClientSubscriptionManager();
        this.serviceSubscriptionManager = new ServiceSubscriptionManager();
        this.servicePacketBufferManager = new ServicePacketBufferManager();
        this.serverNodeManager = new SocketConnectionDataManager(localServerConnectionData);
        this.serverSynchronization = new ServerSynchronizationManager(this.serverNodeManager);
    }

    public SocketConnectionDataManager getServerConnectionDataManager() {
        return this.serverNodeManager;
    }

    public ServicePacketBufferManager getServicePacketBufferManager() {
        return this.servicePacketBufferManager;
    }

    public PacketCategorySubscriptionManager getClientCategorySubscriptionManager() {
        return this.clientSubscriptionManager;
    }

    public PacketCategorySubscriptionManager getServiceSubscriptionManager() {
        return this.serviceSubscriptionManager;
    }

    public ServerSynchronizationManager getServerSynchronizationManager() {
        return this.serverSynchronization;
    }
}
