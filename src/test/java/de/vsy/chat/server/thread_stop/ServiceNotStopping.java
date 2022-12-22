package de.vsy.chat.server.thread_stop;

import static de.vsy.server.data.socketConnection.SocketConnectionState.UNINITIATED;
import static de.vsy.shared_transmission.packet.property.communicator.CommunicationEndpoint.getClientEntity;
import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_SERVER_ID;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

import de.vsy.server.data.ServerDataManager;
import de.vsy.server.data.ServerPersistentDataManager;
import de.vsy.server.data.access.ServiceDataAccessManager;
import de.vsy.server.data.socketConnection.LocalServerConnectionData;
import de.vsy.server.data.socketConnection.RemoteServerConnectionData;
import de.vsy.server.service.ServiceControl;
import de.vsy.server.service.inter_server.InterServerCommunicationService;
import de.vsy.server.service.inter_server.ServerFollowerConnectionEstablisher;
import de.vsy.server.service.request.PacketAssignmentService;
import de.vsy.server.service.status_synchronization.ClientStatusSynchronizationService;
import de.vsy.shared_module.packet_creation.ContentIdentificationProviderImpl;
import de.vsy.shared_module.packet_creation.PacketCompiler;
import de.vsy.shared_module.packet_management.ThreadPacketBufferLabel;
import de.vsy.shared_module.packet_management.ThreadPacketBufferManager;
import de.vsy.shared_module.packet_transmission.ConnectionThreadControl;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutionException;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestServiceNotStopping {

  Logger LOGGER;
  ServerSocket s;
  ServerDataManager serverData;
  ServiceDataAccessManager serviceAccess;
  ServerPersistentDataManager serverPersistentDataManager;

  @BeforeEach
  void initServerPortAndData() throws IOException {

    s = new ServerSocket(6666);
    serverData = new ServerDataManager(LocalServerConnectionData.valueOf(s.getLocalPort(), s));
    serverPersistentDataManager = new ServerPersistentDataManager(
        serverData.getServerConnectionDataManager());
    serverPersistentDataManager.initiatePersistentAccess();
    serviceAccess = new ServiceDataAccessManager(serverData, serverPersistentDataManager);
  }

  @Test
  void TestRequestAssignmentStoppingOnInterrupt() throws InterruptedException {
    var test = new Thread(new PacketAssignmentService(serviceAccess));
    test.start();
    Thread.sleep(1000);
    test.interrupt();
    do {
      Thread.yield();
      LOGGER.info("Warte auf AssignmentService");
    } while (test.isAlive());
    Assertions.assertFalse(test.isAlive());
    LOGGER.info("AssignmentService terminated.");
  }

  @Test
  void TestInterServerCommunicationStoppingOnInterrupt()
      throws InterruptedException, ExecutionException, IOException {
    var asdf = new ThreadPacketBufferManager();
    PacketCompiler.addContentIdentificationProvider(new ContentIdentificationProviderImpl());
    PacketCompiler.addOriginatorEntityProvider(() -> getClientEntity(123456));
    asdf.registerPacketBuffer(ThreadPacketBufferLabel.OUTSIDE_BOUND);
    asdf.registerPacketBuffer(ThreadPacketBufferLabel.SERVER_BOUND);
    asdf.registerPacketBuffer(ThreadPacketBufferLabel.HANDLER_BOUND);
    var d = LocalServerConnectionData.valueOf(12353, s);
    serverData.getServerConnectionDataManager().addServerReceptionConnectionData(d);
    var future = newSingleThreadExecutor().submit(s::accept);

    try (var client = new Socket("127.0.0.1", 6666)) {
      var sasdfasdf = new ConnectionThreadControl(client, asdf);

      try (var connectionSocket = future.get()) {
        serverData.getServerConnectionDataManager().addServerConnection(UNINITIATED,
            RemoteServerConnectionData.valueOf(123, true, connectionSocket));
        var test = new Thread(new InterServerCommunicationService(serviceAccess));
        test.start();
        sasdfasdf.initiateConnectionThreads();
        Thread.sleep(1000);
        test.interrupt();
        do {
          Thread.yield();
          LOGGER.info("Warte auf InterServerService");
        } while (test.isAlive());
      }
    }
    future.cancel(true);
    Assertions.assertTrue(future.isCancelled());
    LOGGER.info("InterServerService terminated.");
  }

  @Test
  void TestrequestAssignmentStoppingOnInterrupt() throws InterruptedException {
    var test = new Thread(new ClientStatusSynchronizationService(serviceAccess));
    test.start();
    Thread.sleep(1000);
    test.interrupt();
    do {
      Thread.yield();
      LOGGER.info("Warte auf AssignmentService");
    } while (test.isAlive());
    Assertions.assertFalse(test.isAlive());
    LOGGER.info("AssignmentService terminated.");
  }

  @Test
  void TestEstablisherStoppingOnInterrupt() throws InterruptedException {
    LocalServerConnectionData conn = LocalServerConnectionData.valueOf(STANDARD_SERVER_ID, s);
    serverData.getServerConnectionDataManager().addServerReceptionConnectionData(conn);
    var test = new Thread(
        new ServerFollowerConnectionEstablisher(serverData.getServerConnectionDataManager(),
            new ServiceControl(serverData, serverPersistentDataManager)));
    test.start();
    Thread.sleep(1000);
    test.interrupt();
    do {
      Thread.yield();
      LOGGER.info("Warte auf ServerFollowerEstablisher");
    } while (test.isAlive());
    Assertions.assertFalse(test.isAlive());
    LOGGER.info("ServerFollowerEstablisher terminated.");
  }
}
