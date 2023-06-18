package de.vsy.chat.server.raw_server_test;

import de.vsy.server.data.*;
import de.vsy.server.data.access.ServerCommunicationServiceDataProvider;
import de.vsy.server.data.socketConnection.LocalServerConnectionData;
import de.vsy.server.data.socketConnection.RemoteServerConnectionData;
import de.vsy.server.persistent_data.server_data.CommunicatorPersistenceDAO;
import de.vsy.server.persistent_data.server_data.temporal.LiveClientStateDAO;
import de.vsy.server.service.ServicePacketBufferManager;
import de.vsy.server.service.inter_server.InterServerCommunicationService;
import de.vsy.shared_module.packet_management.PacketBuffer;
import de.vsy.shared_transmission.packet.Packet;
import org.junit.jupiter.api.Test;

import java.rmi.server.RemoteServer;

public class TestInterServerCommunicationService {
    PacketBuffer assignmentBuffer;
    ClientSubscriptionManager clientSubscriptions;
    ServiceSubscriptionManager serviceSubscriptions;
    ServicePacketBufferManager serviceBuffers;
    LocalServerConnectionData localConnection;
    SocketConnectionDataManager socketConnections;
    ServerSynchronizationManager serverSynchronizationManager;
    @Test
    void testInterServerCommunicationServiceAbortion(){
        var communicationServiceDataProvider = createServerCommunicationServiceDataProvider();
        RemoteServerConnectionData remoteConnection = RemoteServerConnectionData.valueOf(-1, false, null);
        var iscs = new InterServerCommunicationService(communicationServiceDataProvider);
    }

    ServerCommunicationServiceDataProvider createServerCommunicationServiceDataProvider(){
        return new ServerCommunicationServiceDataProvider(){

            @Override
            public PacketCategorySubscriptionManager getClientSubscriptionManager() {
                return clientSubscriptions;
            }

            @Override
            public PacketCategorySubscriptionManager getServiceSubscriptionManager() {
                return serviceSubscriptions;
            }

            @Override
            public LocalServerConnectionData getLocalServerConnectionData() {
                return localConnection;
            }

            @Override
            public ServerSynchronizationManager getServerSynchronizationManager() {
                return serverSynchronizationManager;
            }

            @Override
            public LiveClientStateDAO getLiveClientStateDAO() {
                var liveClientStateDAO = new LiveClientStateDAO(socketConnections);
                liveClientStateDAO.createFileAccess();
                return liveClientStateDAO;
            }

            @Override
            public SocketConnectionDataManager getServerConnectionDataManager() {
                return socketConnections;
            }

            @Override
            public ServicePacketBufferManager getServicePacketBufferManager() {
                return serviceBuffers;
            }

            @Override
            public CommunicatorPersistenceDAO getCommunicatorDataAccessor() {
                var communicatorPersistenceDAO = new CommunicatorPersistenceDAO();
                communicatorPersistenceDAO.createFileAccess();
                return communicatorPersistenceDAO;
            }
        };
    }
}
