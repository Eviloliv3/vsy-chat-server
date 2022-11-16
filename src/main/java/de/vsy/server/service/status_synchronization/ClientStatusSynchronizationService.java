/*
 *
 */
package de.vsy.server.service.status_synchronization;

import static de.vsy.shared_transmission.shared_transmission.packet.property.packet_category.PacketCategory.STATUS;

import de.vsy.server.server.data.AbstractPacketCategorySubscriptionManager;
import de.vsy.server.server.data.access.ClientStatusRegistrationServiceDataProvider;
import de.vsy.server.server_packet.dispatching.PacketDispatcher;
import de.vsy.server.server_packet.dispatching.PacketTransmissionCache;
import de.vsy.server.server_packet.dispatching.ServerSynchronizationPacketDispatcher;
import de.vsy.server.server_packet.packet_creation.ResultingPacketContentHandler;
import de.vsy.server.server_packet.packet_creation.ServerStatusSyncPacketCreator;
import de.vsy.server.service.Service;
import de.vsy.server.service.ServiceBase;
import de.vsy.server.service.ServiceData;
import de.vsy.server.service.ServicePacketBufferManager;
import de.vsy.server.service.packet_logic.ClientStatusPacketProcessorFactory;
import de.vsy.server.service.packet_logic.PacketResponseMap;
import de.vsy.server.service.packet_logic.processor.ServicePacketProcessor;
import de.vsy.shared_module.shared_module.packet_management.PacketBuffer;
import de.vsy.shared_transmission.shared_transmission.packet.Packet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Service processing inter-server synchronization Packet.
 */
public class ClientStatusSynchronizationService extends ServiceBase {

  private static final ServiceData SERVICE_SPECIFICATIONS;
  private static final Logger LOGGER = LogManager.getLogger();

  static {
    SERVICE_SPECIFICATIONS = ServiceData.ServiceDataBuilder.create()
        .withType(Service.TYPE.CHAT_STATUS_UPDATE)
        .withName("ClientSyncService")
        .withDirection(ServiceData.ServiceResponseDirection.INBOUND, Service.TYPE.REQUEST_ROUTER)
        .withDirection(ServiceData.ServiceResponseDirection.OUTBOUND, Service.TYPE.SERVER_TRANSFER)
        .build();
  }

  private final ServicePacketProcessor processor;
  private final AbstractPacketCategorySubscriptionManager serverBoundNetwork;
  private final ServicePacketBufferManager serviceBuffers;
  private final PacketTransmissionCache packetsToSend;
  private final ServerStatusSyncPacketCreator packetCreator;
  private final PacketDispatcher dispatcher;
  private PacketBuffer incomingBuffer;

  /**
   * Instantiates a new client status synchronizing service.
   *
   * @param serviceDataModel the dataManagement manager
   */
  public ClientStatusSynchronizationService(
      final ClientStatusRegistrationServiceDataProvider serviceDataModel) {
    super(SERVICE_SPECIFICATIONS, serviceDataModel.getServicePacketBufferManager(),
        serviceDataModel.getLocalServerConnectionData());
    this.serviceBuffers = serviceDataModel.getServicePacketBufferManager();
    this.serverBoundNetwork = serviceDataModel.getServiceSubscriptionManager();
    this.packetCreator = new ServerStatusSyncPacketCreator();
    this.packetsToSend = new PacketTransmissionCache();
    var resultingPackets = new ResultingPacketContentHandler(packetCreator, packetsToSend);
    this.processor = new ServicePacketProcessor(
        new ClientStatusPacketProcessorFactory(resultingPackets, serviceDataModel),
        resultingPackets);
    this.dispatcher = new ServerSynchronizationPacketDispatcher(this.serviceBuffers,
        SERVICE_SPECIFICATIONS.getResponseDirections());
  }

  /**
   * Finish setup.
   */
  @Override
  public void finishSetup() {
    final var serviceId = super.getServiceId();

    this.incomingBuffer = this.serviceBuffers.registerBuffer(
        SERVICE_SPECIFICATIONS.getServiceType(), serviceId);
    this.serverBoundNetwork.subscribe(STATUS, serviceId, incomingBuffer);
    super.setReadyState();
  }

  /**
   * Work.
   */
  @Override
  public void work() {
    Packet input;

    try {
      input = incomingBuffer.getPacket();
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      LOGGER.error("Interrupted while waiting for next Packet.");
      input = null;
    }

    if (input != null) {
      this.packetCreator.setCurrentPacket(input);
      this.processor.processPacket(input);
      this.packetsToSend.transmitPackets(this.dispatcher);
    }
  }

  /**
   * Break down.
   */
  @Override
  public void breakDown() {
    this.serviceBuffers.deregisterBuffer(getServiceType(), getServiceId(), this.incomingBuffer);
  }

  /**
   * Dispatch response Packetmap.
   *
   * @param responseMap the response map
   */
  private void dispatchResponsePacketMap(final PacketResponseMap responseMap) {
    Packet toDispatch;

    if (responseMap != null) {
      toDispatch = responseMap.getClientBoundPacket();

      if (toDispatch != null) {
        this.dispatcher.dispatchPacket(toDispatch);
      }
      toDispatch = responseMap.getServerBoundPacket();

      if (toDispatch != null) {
        this.dispatcher.dispatchPacket(toDispatch);
      }
    }
  }
}
