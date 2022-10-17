package de.vsy.chat.server.testing_grounds;

import de.vsy.shared_module.shared_module.packet_management.ThreadPacketBufferLabel;
import de.vsy.shared_module.shared_module.packet_management.ThreadPacketBufferManager;
import de.vsy.shared_module.shared_module.packet_transmission.ConnectionThreadControl;
import de.vsy.shared_module.shared_module.packet_transmission.PacketReadThread;
import de.vsy.shared_module.shared_module.packet_transmission.cache.UnconfirmedPacketTransmissionCache;
import de.vsy.shared_transmission.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.shared_transmission.packet.PacketBuilder;
import de.vsy.shared_transmission.shared_transmission.packet.content.authentication.LoginRequestDTO;
import de.vsy.shared_transmission.shared_transmission.packet.property.PacketPropertiesBuilder;
import de.vsy.shared_transmission.shared_transmission.packet.property.communicator.CommunicationEndpoint;
import de.vsy.shared_transmission.shared_transmission.packet.property.packet_identifier.AuthenticationIdentifier;
import de.vsy.shared_transmission.shared_transmission.packet.property.packet_type.AuthenticationType;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestConnectionThreads {

  Socket serverSocket;
  Socket clientSocket;
  ThreadPacketBufferManager bufferManagerOne;
  ThreadPacketBufferManager bufferManagerTwo;

  public TestConnectionThreads() {
  }

  @BeforeEach
  public void setupConnections() throws IOException, InterruptedException, ExecutionException {
    bufferManagerOne = new ThreadPacketBufferManager();
    bufferManagerTwo = new ThreadPacketBufferManager();

    bufferManagerOne.registerPacketBuffer(ThreadPacketBufferLabel.SERVER_BOUND);
    bufferManagerOne.registerPacketBuffer(ThreadPacketBufferLabel.HANDLER_BOUND);
    bufferManagerOne.registerPacketBuffer(ThreadPacketBufferLabel.OUTSIDE_BOUND);

    bufferManagerTwo.registerPacketBuffer(ThreadPacketBufferLabel.SERVER_BOUND);
    bufferManagerTwo.registerPacketBuffer(ThreadPacketBufferLabel.HANDLER_BOUND);
    bufferManagerTwo.registerPacketBuffer(ThreadPacketBufferLabel.OUTSIDE_BOUND);

    try (var connectionControl = new ServerSocket(12345)) {
      var connectionSocket = Executors.newSingleThreadExecutor().submit(connectionControl::accept);
      clientSocket = new Socket("127.0.0.1", 12345);
      serverSocket = connectionSocket.get();
    }
  }

  @AfterEach
  public void closeConnections() throws IOException {
    clientSocket.close();
    serverSocket.close();
  }

  @Test
  void sendSingleRead() throws InterruptedException, IOException {
    PacketBuilder builder = new PacketBuilder();
    PacketPropertiesBuilder propertiesBuilder = new PacketPropertiesBuilder();
    propertiesBuilder.withIdentifier(new AuthenticationIdentifier(AuthenticationType.CLIENT_LOGIN))
        .withRecipient(CommunicationEndpoint.getClientEntity(123456))
        .withSender(CommunicationEndpoint.getClientEntity(143456));
    var loginRequest = new LoginRequestDTO("test", "test");
    builder.withProperties(propertiesBuilder.build()).withContent(loginRequest);

    final var transmissionCache = new UnconfirmedPacketTransmissionCache(1000);
    final var objectOut = new ObjectOutputStream(clientSocket.getOutputStream());
    final var reader = new PacketReadThread(transmissionCache, serverSocket.getInputStream(),
        bufferManagerOne);
    Thread readThread = new Thread(reader);
    readThread.start();
    objectOut.writeObject(builder.build());
    final var packet = bufferManagerOne.getPacketBuffer(ThreadPacketBufferLabel.OUTSIDE_BOUND)
        .getPacket();
    readThread.interrupt();
    LogManager.getLogger().debug(packet);
    Assertions.assertNotNull(packet.getPacketContent());
  }

  @Test
  void sendSingleConnection() throws IOException, ClassNotFoundException {
    final var connectionControl = new ConnectionThreadControl(serverSocket, bufferManagerOne, true);
    final var objectOut = new ObjectOutputStream(clientSocket.getOutputStream());
    connectionControl.initiateConnectionThreads();
    final var objectIn = new ObjectInputStream(clientSocket.getInputStream());
    final var builder = new PacketBuilder();
    final var propertiesBuilder = new PacketPropertiesBuilder();
    final var loginRequest = new LoginRequestDTO("test", "test");

    propertiesBuilder.withIdentifier(new AuthenticationIdentifier(AuthenticationType.CLIENT_LOGIN))
        .withRecipient(CommunicationEndpoint.getClientEntity(123456))
        .withSender(CommunicationEndpoint.getClientEntity(143456));
    builder.withProperties(propertiesBuilder.build()).withContent(loginRequest);

    objectOut.writeObject(builder.build());
    Object ack = objectIn.readObject();
    if (ack instanceof Packet pack) {
      LogManager.getLogger().debug(pack);
      Assertions.assertNull(pack.getPacketContent());
    } else {
      Assertions.assertFalse((false));
    }
    connectionControl.closeConnection();
    objectOut.close();
    objectIn.close();
  }

  @Test
  void sendTwoConnectionThreads() throws InterruptedException {
    final var serverConnectionControl = new ConnectionThreadControl(serverSocket, bufferManagerOne,
        true);
    final var clientConnectionControl = new ConnectionThreadControl(clientSocket, bufferManagerTwo);
    final var builder = new PacketBuilder();
    final var propertiesBuilder = new PacketPropertiesBuilder();
    final var loginRequest = new LoginRequestDTO("test", "test");

    Thread s = new Thread(serverConnectionControl::initiateConnectionThreads);
    s.start();

    clientConnectionControl.initiateConnectionThreads();

    propertiesBuilder.withIdentifier(new AuthenticationIdentifier(AuthenticationType.CLIENT_LOGIN))
        .withRecipient(CommunicationEndpoint.getClientEntity(123456))
        .withSender(CommunicationEndpoint.getClientEntity(143456));
    builder.withProperties(propertiesBuilder.build()).withContent(loginRequest);

    bufferManagerTwo.getPacketBuffer(ThreadPacketBufferLabel.OUTSIDE_BOUND)
        .appendPacket(builder.build());
    Packet pack = bufferManagerOne.getPacketBuffer(ThreadPacketBufferLabel.HANDLER_BOUND)
        .getPacket();

    LogManager.getLogger().debug(pack);
    Assertions.assertNotNull(pack.getPacketContent());

    serverConnectionControl.closeConnection();
    clientConnectionControl.closeConnection();
  }
}