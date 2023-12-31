package de.vsy.server.service;

import de.vsy.server.ChatServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.TimerTask;

public class ServiceHealthMonitor extends TimerTask {

    private static final Logger LOGGER = LogManager.getLogger();
    private final ChatServer server;
    private final ServiceControl services;
    private boolean serverShutdownInitiated;

    public ServiceHealthMonitor(final ChatServer server, final ServiceControl services) {
        this.server = server;
        this.services = services;
        this.serverShutdownInitiated = false;
    }

    @Override
    public void run() {
        if (!(this.services.confinedServicesHealthy())) {
            if (!(serverShutdownInitiated)) {
                LOGGER.info("Local services failed. Server termination initiated.");
                this.server.shutdownServer();
                serverShutdownInitiated = true;
            }
        }
    }
}
