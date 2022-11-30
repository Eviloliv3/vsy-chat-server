/*
 *
 */
package de.vsy.server.service;

import de.vsy.shared_module.packet_management.PacketBuffer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Manages all PacketBuffers offered by services for subscription.
 */
public class ServicePacketBufferManager {

  private static final ThreadLocalRandom RANDOM_NUMBER_GENERATOR;
  private static final Logger LOGGER = LogManager.getLogger();

  static {
    RANDOM_NUMBER_GENERATOR = ThreadLocalRandom.current();
  }

  private final ReadWriteLock lock;
  private final Map<Service.TYPE, Map<Integer, PacketBuffer>> registeredBuffers;

  /**
   * Instantiates a new service PacketBuffer manager.
   */
  public ServicePacketBufferManager() {
    this(new EnumMap<>(Service.TYPE.class));
  }

  /**
   * Instantiates a new service PacketBuffer manager.
   *
   * @param bufferMap the buffer map
   */
  public ServicePacketBufferManager(final Map<Service.TYPE, Map<Integer, PacketBuffer>> bufferMap) {

    this.registeredBuffers = bufferMap;
    this.lock = new ReentrantReadWriteLock();
  }

  /**
   * Deregister buffer.
   *
   * @param serviceType  the service type
   * @param bufferLabel  the buffer label
   * @param packetBuffer the PacketBuffer
   * @return the PacketBuffer
   */
  public boolean deregisterBuffer(final Service.TYPE serviceType, final int bufferLabel,
      final PacketBuffer packetBuffer) {
    var bufferDeregistered = false;
    Map<Integer, PacketBuffer> bufferMap;

    try {
      this.lock.writeLock().lock();
      bufferMap = this.registeredBuffers.get(serviceType);

      if (bufferMap != null) {
        bufferDeregistered = bufferMap.remove(bufferLabel, packetBuffer);
      } else {
        LOGGER.info("No PacketBuffer for service type: {}", serviceType);
      }
    } finally {
      this.lock.writeLock().unlock();
    }
    LOGGER.info("PacketBuffer deregistered: Service - {}/Label - {}", serviceType, bufferLabel);

    return bufferDeregistered;
  }

  /**
   * Returns the random buffer.
   *
   * @param serviceType the service type
   * @return the random buffer
   */
  public PacketBuffer getRandomBuffer(final Service.TYPE serviceType) {
    PacketBuffer buffer = null;
    List<PacketBuffer> bufferList;

    try {
      this.lock.readLock().lock();
      bufferList = getAllBuffersFor(serviceType);

      if (bufferList != null && !bufferList.isEmpty()) {
        buffer = bufferList.get(RANDOM_NUMBER_GENERATOR.nextInt(bufferList.size()));
      }
    } finally {
      this.lock.readLock().unlock();
    }
    return buffer;
  }

  /**
   * Returns the all buffers for.
   *
   * @param serviceType the service type
   * @return the all buffers for
   */
  public List<PacketBuffer> getAllBuffersFor(final Service.TYPE serviceType) {
    Map<Integer, PacketBuffer> bufferMap;
    final List<PacketBuffer> foundBuffers = new ArrayList<>();

    try {
      this.lock.readLock().lock();

      if (serviceType != null && this.registeredBuffers != null) {
        bufferMap = this.registeredBuffers.get(serviceType);

        if (bufferMap != null) {
          foundBuffers.addAll(bufferMap.values());
        }
      }
    } finally {
      this.lock.readLock().unlock();
    }
    return foundBuffers;
  }

  /**
   * Returns the specific buffer.
   *
   * @param serviceType the service type
   * @param bufferLabel the buffer label
   * @return the specific buffer
   */
  public PacketBuffer getSpecificBuffer(final Service.TYPE serviceType, final int bufferLabel) {
    PacketBuffer buffer = null;
    Map<Integer, PacketBuffer> bufferMap;

    try {
      this.lock.readLock().lock();
      bufferMap = this.registeredBuffers.get(serviceType);

      if (bufferMap != null) {
        buffer = bufferMap.get(bufferLabel);
      }
    } finally {
      this.lock.readLock().unlock();
    }
    return buffer;
  }

  /**
   * Register buffer manager.
   *
   * @param serviceType the service type
   * @return the PacketBuffer
   */
  public PacketBuffer registerBuffer(final Service.TYPE serviceType, final int serviceId) {
    return registerBuffer(serviceType, serviceId, new PacketBuffer());
  }

  /**
   * Register buffer manager.
   *
   * @param serviceType  Service.TYPE
   * @param bufferLabel  int
   * @param packetBuffer PacketBuffer
   * @return PacketBuffer
   */
  public PacketBuffer registerBuffer(final Service.TYPE serviceType, final int bufferLabel,
      final PacketBuffer packetBuffer) {
    PacketBuffer bufferRegistered = null;

    if (serviceType != null && packetBuffer != null) {

      try {
        this.lock.writeLock().lock();
        var serviceBufferMap = this.registeredBuffers.computeIfAbsent(serviceType,
            service -> new HashMap<>());
        final var previousValue = serviceBufferMap.putIfAbsent(bufferLabel, packetBuffer);

        if (previousValue == null) {
          this.registeredBuffers.put(serviceType, serviceBufferMap);
          bufferRegistered = packetBuffer;
        } else {
          LOGGER.info("Buffer already registered service type/hash: {}/{}", serviceType,
              packetBuffer.toString());
        }
      } finally {
        this.lock.writeLock().unlock();
      }
    }
    LOGGER.info("Buffer registered: service/label - {}/{}", serviceType, bufferLabel);

    return bufferRegistered;
  }
}
