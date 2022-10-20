/*
 *
 */
package de.vsy.server.service;

import de.vsy.server.server.data.ServerDataManager;
import de.vsy.server.server.data.ServerPersistentDataManager;
import de.vsy.server.server.data.access.ServiceDataAccessManager;
import de.vsy.server.service.Service.TYPE;
import de.vsy.server.service.inter_server.InterServerCommunicationService;
import de.vsy.server.service.inter_server.InterServerSocketConnectionEstablisher;
import de.vsy.server.service.request.PacketAssignmentService;
import de.vsy.server.service.status_synchronization.ClientStatusSynchronizationService;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServiceControl {

  private static final Logger LOGGER = LogManager.getLogger();
  private final Map<Service.TYPE, Set<Thread>> registeredServices;
  private InterServerSocketConnectionEstablisher interServerConnectionEstablisher;
  private ServiceDataAccessManager serviceDataModel;

  /**
   * Instantiates a new service control.
   *
   * @param serverData                  the server dataManagement
   * @param serverPersistentDataManager the server persistence access
   */
  public ServiceControl(final ServerDataManager serverData,
      final ServerPersistentDataManager serverPersistentDataManager) {

    serviceDataModel = new ServiceDataAccessManager(serverData, serverPersistentDataManager);
    this.registeredServices = new EnumMap<>(Service.TYPE.class);
  }

  /**
   * All services healthy.
   *
   * @return true, if successful
   */
  public boolean confinedServicesHealthy() {
    for (final var currentThreadSet : this.registeredServices.entrySet()) {
      final var currentServiceType = currentThreadSet.getKey();

      if(!(currentServiceType.equals(TYPE.SERVER_TRANSFER))) {
        final var currentServiceThreads = currentThreadSet.getValue();

        for (final var currentThread : currentServiceThreads) {

          if (!currentThread.isAlive() || currentThread.isInterrupted()) {
            return false;
          }
        }
      }
    }
    return true;
  }

  /**
   * Start services.
   */
  public void startServices() {
    startAssignmentThread();
    startClientStatusSynchronizationThread();
    this.interServerConnectionEstablisher = new InterServerSocketConnectionEstablisher(
        this.serviceDataModel.getServerConnectionDataManager(), this);
    interServerConnectionEstablisher.establishConnections();
  }

  /**
   * Start assignment thread.
   */
  public void startAssignmentThread() {
    final var as = new PacketAssignmentService(this.serviceDataModel);
    startService(as);
  }

  /**
   * Start client status synchronization thread.
   */
  public void startClientStatusSynchronizationThread() {
    final var csss = new ClientStatusSynchronizationService(this.serviceDataModel);
    startService(csss);
  }

  /**
   * Start service.
   *
   * @param newService the new service
   */
  private void startService(final Service newService) {
    Set<Thread> sameTypeServices = this.registeredServices.computeIfAbsent(
        newService.getServiceType(),
        serviceType -> new HashSet<>());
    Thread newServiceThread = new Thread(newService);
    sameTypeServices.add(newServiceThread);
    newServiceThread.start();
    this.registeredServices.put(newService.getServiceType(), sameTypeServices);

    do {
      Thread.yield();
    } while (newServiceThread.isAlive() && !newService.getReadyState() && !Thread.currentThread()
        .isInterrupted());
  }

  /**
   * Start inter-server comm thread.
   */
  public void startInterServerCommThread() {
    final var iscr = new InterServerCommunicationService(this.serviceDataModel);
    startService(iscr);
  }

  public void stopAllServices() {
    LOGGER.info("Services werden gestoppt.");
    this.interServerConnectionEstablisher.stopEstabilishingConnections();

    for (final var serviceSet : this.registeredServices.entrySet()) {
      LOGGER.info("{}-Services werden beendet", serviceSet.getKey());

      for (final var currentService : serviceSet.getValue()) {
        final var threadName = currentService.getName();
        LOGGER.info("Service wird beendet: {}", currentService.getName());
        currentService.interrupt();

        do {
          LOGGER.info("Warte noch auf {}", threadName);
          Thread.yield();
        } while (currentService.isAlive());
      }
    }
    LOGGER.info("Services wurden beendet.");
  }

  /**
   * Stop service.
   *
   * @param serviceType the service type
   * @param threadName  the thread name
   * @return true, if successful
   */
  public boolean stopService(final Service.TYPE serviceType, final String threadName) {
    Set<Thread> serviceThreads;

    if (serviceType == null || threadName == null) {
      return false;
    }
    serviceThreads = this.registeredServices.get(serviceType);

    if (serviceThreads != null) {

      for (final Thread service : serviceThreads) {

        if (service.getName().equals(threadName)) {
          service.interrupt();
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Sets the up service dataManagement manager.
   */
  private void setupServiceDataManager(final ServiceDataAccessManager serviceDataManager) {
    this.serviceDataModel = serviceDataManager;
  }
}
