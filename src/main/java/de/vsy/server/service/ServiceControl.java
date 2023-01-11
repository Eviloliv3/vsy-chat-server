/*
 *
 */
package de.vsy.server.service;

import de.vsy.server.data.ServerDataManager;
import de.vsy.server.data.ServerPersistentDataManager;
import de.vsy.server.data.access.ServiceDataAccessManager;
import de.vsy.server.service.Service.TYPE;
import de.vsy.server.service.inter_server.InterServerCommunicationService;
import de.vsy.server.service.inter_server.InterServerCommunicationServiceCreator;
import de.vsy.server.service.inter_server.InterServerSocketConnectionEstablisher;
import de.vsy.server.service.request.PacketAssignmentService;
import de.vsy.server.service.status_synchronization.ClientStatusSynchronizationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ServiceControl implements InterServerCommunicationServiceCreator {

    private static final Logger LOGGER = LogManager.getLogger();
    private final Map<Service.TYPE, Set<Thread>> registeredServices;
    private final ServiceDataAccessManager serviceDataModel;
    private InterServerSocketConnectionEstablisher interServerConnectionEstablisher;

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
        if (!this.interServerConnectionEstablisher.isEstablishingConnections()) {
            return false;
        }

        for (final var currentThreadSet : this.registeredServices.entrySet()) {
            final var currentServiceType = currentThreadSet.getKey();

            if (!(currentServiceType.equals(TYPE.SERVER_TRANSFER))) {
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
        startInterServerCommServices();
    }

    private void startInterServerCommServices() {
        var remoteConnections = this.serviceDataModel.getServerConnectionDataManager();

        while (remoteConnections.uninitiatedConnectionsRemaining()) {
            startInterServerCommThread();
        }

    }

    public void startInterServerConnector() {
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

        try {
            newService.waitForServiceReadiness();
        } catch (InterruptedException ie) {
            LOGGER.error("Interrupted while waiting for readiness: {}:", newService.getServiceName());
        }
    }

    /**
     * Start inter-server comm thread.
     */
    public void startInterServerCommThread() {
        final var iscr = new InterServerCommunicationService(this.serviceDataModel);
        startService(iscr);
    }

    public void stopAllServices() {
        LOGGER.info("Services termination initiated.");
        this.interServerConnectionEstablisher.stopEstablishingConnections();

        for (final var serviceSet : this.registeredServices.entrySet()) {

            for (final var currentService : serviceSet.getValue()) {
                final var threadName = currentService.getName();
                LOGGER.info("Service termination initiated: {}", currentService.getName());
                currentService.interrupt();

                try {
                    currentService.join(500);
                } catch (InterruptedException e) {
                    LOGGER.error("Service shutdown failed.", e);
                }
                LOGGER.info("{} terminated.", threadName);
            }
        }
        LOGGER.info("Services terminated.");
    }
}
