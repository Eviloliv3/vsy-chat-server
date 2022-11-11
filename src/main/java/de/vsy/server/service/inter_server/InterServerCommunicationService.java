/*
 *
 */
package de.vsy.server.service.inter_server;

import static de.vsy.server.server.data.socketConnection.SocketConnectionState.INITIATED;
import static de.vsy.shared_transmission.shared_transmission.packet.property.communicator.CommunicationEndpoint.getServerEntity;
import static java.util.Arrays.asList;

import de.vsy.server.exception_processing.ServerPacketHandlingExceptionCreator;
import de.vsy.server.server.data.access.ServerCommunicationServiceDataProvider;
import de.vsy.server.server.data.socketConnection.RemoteServerConnectionData;
import de.vsy.server.server.data.SocketConnectionDataManager;
import de.vsy.server.server_packet.content.InterServerCommSyncDTO;
import de.vsy.server.server_packet.content.ServerPacketContentImpl;
import de.vsy.server.server_packet.content.builder.ServerFailureContentBuilder;
import de.vsy.server.server_packet.dispatching.InterServerCommunicationPacketDispatcher;
import de.vsy.server.server_packet.dispatching.PacketDispatcher;
import de.vsy.server.server_packet.packet_validation.ServerPacketTypeValidationCreator;
import de.vsy.server.service.RemotePacketBuffer;
import de.vsy.server.service.Service;
import de.vsy.server.service.ServiceBase;
import de.vsy.server.service.ServiceData;
import de.vsy.server.service.ServiceData.ServiceDataBuilder;
import de.vsy.server.service.ServiceData.ServiceResponseDirection;
import de.vsy.shared_module.shared_module.exception_processing.PacketHandlingExceptionProcessor;
import de.vsy.shared_module.shared_module.packet_creation.PacketCompiler;
import de.vsy.shared_module.shared_module.packet_exception.PacketProcessingException;
import de.vsy.shared_module.shared_module.packet_management.PacketBuffer;
import de.vsy.shared_module.shared_module.packet_management.ThreadPacketBufferLabel;
import de.vsy.shared_module.shared_module.packet_management.ThreadPacketBufferManager;
import de.vsy.shared_module.shared_module.packet_transmission.ConnectionThreadControl;
import de.vsy.shared_module.shared_module.packet_transmission.cache.UnconfirmedPacketTransmissionCache;
import de.vsy.shared_module.shared_module.packet_validation.PacketCheck;
import de.vsy.shared_module.shared_module.packet_validation.SimplePacketChecker;
import de.vsy.shared_module.shared_module.thread_manipulation.ProcessingInterruptProvider;
import de.vsy.shared_transmission.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.shared_transmission.packet.content.PacketContent;
import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Service handling communication between ChatServers. Initiates server synchronization on new
 * connection. Performs semantic checks on read Packet. Also creates notification in case the
 * connection fails.
 */
public class InterServerCommunicationService extends ServiceBase {

  private static final ServiceData SERVICE_SPECIFICATIONS;
  private static final Logger LOGGER = LogManager.getLogger();

  static {
    SERVICE_SPECIFICATIONS = ServiceDataBuilder.create().withType(Service.TYPE.SERVER_TRANSFER)
        .withName("InterServerCommunicationService")
        .withDirection(ServiceResponseDirection.INBOUND, Service.TYPE.REQUEST_ROUTER)
        .withDirection(ServiceResponseDirection.OUTBOUND, Service.TYPE.SERVER_TRANSFER).build();
  }

  private final SocketConnectionDataManager serverConnectionDataManager;
  private final UnconfirmedPacketTransmissionCache packetCache;
  private final PacketHandlingExceptionProcessor pheProcessor;
  private final ServerCommunicationServiceDataProvider serviceDataAccess;
  private final ThreadPacketBufferManager threadBuffers;
  private ConnectionThreadControl connectionControl;
  private PacketDispatcher packetDispatcher;
  private RemoteServerConnectionData remoteConnectionData;
  private ProcessingInterruptProvider localInterruptor;
  private PacketCheck validator;

  /**
   * Instantiates a new InterServerCommService.
   *
   * @param serviceDataAccess the service dataManagement access
   */
  public InterServerCommunicationService(
      final ServerCommunicationServiceDataProvider serviceDataAccess) {
    super(SERVICE_SPECIFICATIONS, serviceDataAccess.getServicePacketBufferManager(),
        serviceDataAccess.getLocalServerConnectionData());
    this.serverConnectionDataManager = serviceDataAccess.getServerConnectionDataManager();
    this.serviceDataAccess = serviceDataAccess;
    this.threadBuffers = new ThreadPacketBufferManager();
    this.pheProcessor = ServerPacketHandlingExceptionCreator.getServiceExceptionProcessor();
    this.packetCache = new UnconfirmedPacketTransmissionCache(1000);
  }

  @Override
  public void finishSetup() {
    this.remoteConnectionData = this.serverConnectionDataManager.getNextSocketConnectionToInitiate();

    if (this.remoteConnectionData == null){
      Thread.currentThread().interrupt();
      LOGGER.error("No connection data found. {} interrupt flag set.", Thread.currentThread().getName());
      setReadyState();
      return;
    }
    this.validator = new SimplePacketChecker(
        ServerPacketTypeValidationCreator.createRegularServerPacketContentValidator());
    this.localInterruptor = this::interruptionConditionNotMet;

    setupThreadPacketBufferManager();
    this.connectionControl = new ConnectionThreadControl(remoteConnectionData.getConnectionSocket(),
        this.threadBuffers, this.packetCache, this.remoteConnectionData.isLeader());

    if (this.connectionControl.initiateConnectionThreads()) {
      this.packetDispatcher = new InterServerCommunicationPacketDispatcher(
          this.remoteConnectionData,
          this.serviceDataAccess.getServicePacketBufferManager(),
          SERVICE_SPECIFICATIONS.getResponseDirections(),
          this.threadBuffers.getPacketBuffer(ThreadPacketBufferLabel.OUTSIDE_BOUND));
    } else {
      Thread.currentThread().interrupt();
      LOGGER.error("Connection initiation failed. {}", this.remoteConnectionData);
    }
  }

  @Override
  public boolean interruptionConditionNotMet() {
    return !Thread.currentThread().isInterrupted() && this.connectionControl.connectionIsLive();
  }

  @Override
  public void work() {
    synchronizeInterServerCommService();
    makeServiceAvailable();
    waitForServerSynchronization();
    continuouslyProcessInput(this.localInterruptor);

    if (!this.connectionControl.connectionIsLive() && !Thread.currentThread().isInterrupted()) {
      setupSubstituteService();
    }
  }

  @Override
  public void breakDown() {

    if (this.connectionControl != null) {
      this.connectionControl.closeConnection();
      try {
        this.remoteConnectionData.closeConnection();
      } catch (IOException ioe) {
        LOGGER.info("Error while closing connection:\n{}", ioe.getLocalizedMessage());
      }
    }
    this.serverConnectionDataManager.removeServerConnection(this.remoteConnectionData);

    this.serviceDataAccess.getServicePacketBufferManager()
        .deregisterBuffer(getServiceType(), getServiceId(),
            this.threadBuffers.getPacketBuffer(ThreadPacketBufferLabel.OUTSIDE_BOUND));
  }

  /**
   * Sendet den Serverport des.
   */
  private void synchronizeInterServerCommService() {
    var synchronizationPacket = createInterServerSyncPacket();
    final var inputBuffer = this.threadBuffers.getPacketBuffer(
        ThreadPacketBufferLabel.HANDLER_BOUND);

    this.threadBuffers.getPacketBuffer(ThreadPacketBufferLabel.OUTSIDE_BOUND)
        .appendPacket(synchronizationPacket);

    while (interruptionConditionNotMet()) {

      try {
        synchronizationPacket = inputBuffer.getPacket();
      } catch (InterruptedException ie) {
        LOGGER.error("Interrupted while waiting for next packet.\n{}",
            asList(ie.getStackTrace()));
        Thread.currentThread().interrupt();
        synchronizationPacket = null;
      }

      if (synchronizationPacket != null) {
        final var validatorString = validator.checkPacket(synchronizationPacket);
        final var content = synchronizationPacket.getPacketContent();

        if (validatorString.isPresent()) {
          LOGGER.error("Packet validation error: {}", validatorString.get());
          continue;
        }

        if (content instanceof final InterServerCommSyncDTO synchronizationContent) {
          completeRemoteConnectionData(synchronizationContent);
          LOGGER.info("Connection created {}", synchronizationContent.getServerId());
          return;
        }
      }
    }
    LOGGER.info("Server connection synchronized.");
  }

  /**
   * Der InterServerCommunicationService traegt seinen Buffer in ServicePacketBufferManager ein und
   * setzt das Service-readyFlag.
   */
  private void makeServiceAvailable() {
    final var remoteServerId = this.remoteConnectionData.getServerId();
    this.serviceDataAccess.getServicePacketBufferManager()
        .registerBuffer(SERVICE_SPECIFICATIONS.getServiceType(),
            remoteServerId,
            this.threadBuffers.getPacketBuffer(ThreadPacketBufferLabel.OUTSIDE_BOUND));
    super.setReadyState();
  }

  /**
   * Wartet bis der Hauptthread des Servers die Synchronisation der Klientenzustände signalisiert
   * oder der Service unterbrochen wird. Eine Unterbrechung wird aufgefrischt.
   */
  private void waitForServerSynchronization() {
    LOGGER.info("Waiting for server synchronization.");
    try {
      this.serviceDataAccess.getServerSynchronizationManager().waitForClientSynchronization();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    LOGGER.info("Finished waiting for server synchronization.");
  }

  /**
   * Prüft aus dem Netz eingehende Pakete und schreibt sie auf den RequestAssignmentBuffer weiter.
   * Die Abbruchbedingung wird als Parameter übergeben.
   *
   * @param interrupt the interrupt
   */
  private void continuouslyProcessInput(final ProcessingInterruptProvider interrupt) {
    LOGGER.info("Processing incoming packets.");
    var reinterrupt = false;
    final var inputBuffer = this.threadBuffers.getPacketBuffer(
        ThreadPacketBufferLabel.HANDLER_BOUND);

    while (interrupt.conditionNotMet()) {

      try {
        final var input = inputBuffer.getPacket();

        if (input != null) {
          processPacket(input);
        }
      } catch (InterruptedException ie) {
        LOGGER.error("Interrupted while waiting for next packet.\n{}",
            asList(ie.getStackTrace()));
        reinterrupt = true;
      }
    }

    if (reinterrupt) {
      Thread.currentThread().interrupt();
    }
    LOGGER.info("Packet processing ended.");
  }

  private void setupSubstituteService() {
    Thread substituteThread;

    relaisConnectedServerFailure();
    emptyInputBuffer();
    substituteThread = new Thread(new InterServerSubstituteService(this.serviceDataAccess,
        this.remoteConnectionData,
        this.threadBuffers.getPacketBuffer(ThreadPacketBufferLabel.OUTSIDE_BOUND)));
    substituteThread.start();
  }

  /**
   * Erstellt ein Paket zur Synchronisation des genutzten Serverports.
   *
   * @return the packet
   */
  private Packet createInterServerSyncPacket() {
    final var recipient = getServerEntity(this.remoteConnectionData.getServerId());

    final PacketContent synchronizedContent = new InterServerCommSyncDTO(
        this.serverConnectionDataManager.getLocalServerConnectionData().getServerId());

    return PacketCompiler.createRequest(recipient, synchronizedContent);
  }

  void completeRemoteConnectionData(InterServerCommSyncDTO remoteServerSyncData) {
    final var remotePacketBuffer = (RemotePacketBuffer) this.threadBuffers
        .getPacketBuffer(ThreadPacketBufferLabel.OUTSIDE_BOUND);
    final var currentRemoteConnectionData = this.remoteConnectionData;
    this.remoteConnectionData = RemoteServerConnectionData.valueOf(
        remoteServerSyncData.getServerId(),
        this.remoteConnectionData.isLeader(), this.remoteConnectionData.getConnectionSocket());

    remotePacketBuffer.updateRemoteConnectionData(this.remoteConnectionData);
    this.remoteConnectionData.setRemoteServerConnector(remotePacketBuffer);

    this.serverConnectionDataManager.addServerConnection(INITIATED, this.remoteConnectionData);
    this.serverConnectionDataManager.removeServerConnection(currentRemoteConnectionData);
  }

  private void processPacket(final Packet nextPacket) {
    final Packet output;
    final var validationString = this.validator.checkPacket(nextPacket);

    if (validationString.isEmpty()) {
      final var serverPacketContent = (ServerPacketContentImpl) nextPacket.getPacketContent();
      serverPacketContent.setReadingConnectionThread(super.getServiceId());
      output = nextPacket;
    } else {
      final var errorMessage = "Das Paket wurde nicht zugestellt. ";
      final var processingException = new PacketProcessingException(
          errorMessage + validationString.get());
      output = this.pheProcessor.processException(processingException, nextPacket);
    }
    this.packetDispatcher.dispatchPacket(output);
  }

  /**
   * Relais connected server failure.
   *
   * @return true, if successful
   */
  private boolean relaisConnectedServerFailure() {
    Packet failureNotification;
    final var failureContent = new ServerFailureContentBuilder()
        .withFailedServerId(this.remoteConnectionData.getServerId()).build();
    /*
     * final var recipient = CommunicationEndpoint.getServerEntity(
     * this.remoteConnectionData.getServerId()); final var assignmentBuffer =
     * this.threadBuffers.getPacketBuffer( ThreadPacketBufferLabel.SERVER_BOUND);
     *
     * if (assignmentBuffer != null) { failureNotification =
     * PacketCompiler.createRequest(recipient, failureContent); return
     * assignmentBuffer.appendPacket(failureNotification); } else { LOGGER.error(
     * "Verbundener Server ausgefallen. Kein Assignment PacketBuffer gefunden (Statusmeldung verfaellt)."
     * ); return false; }
     */
    LOGGER.info("Connection to remote client interrupted: {}", failureContent);
    return true;
  }

  private void emptyInputBuffer() {
    final ProcessingInterruptProvider interrupt = () -> this.threadBuffers
        .getPacketBuffer(ThreadPacketBufferLabel.HANDLER_BOUND).containsPackets();

    continuouslyProcessInput(interrupt);
  }

  private void setupThreadPacketBufferManager() {
    PacketBuffer bufferToRegister;

    bufferToRegister = this.serviceDataAccess.getServicePacketBufferManager()
        .getRandomBuffer(Service.TYPE.REQUEST_ROUTER);
    this.threadBuffers.setPacketBuffer(ThreadPacketBufferLabel.SERVER_BOUND, bufferToRegister);
    this.threadBuffers.registerPacketBuffer(ThreadPacketBufferLabel.HANDLER_BOUND);
    bufferToRegister = new RemotePacketBuffer(
        this.serverConnectionDataManager.getLocalServerConnectionData(),
        this.remoteConnectionData);
    this.threadBuffers.setPacketBuffer(ThreadPacketBufferLabel.OUTSIDE_BOUND, bufferToRegister);
  }
}
