package de.vsy.server.service;

import de.vsy.server.data.socketConnection.LocalServerConnectionData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static de.vsy.shared_utility.standard_value.ThreadContextValues.*;

/**
 * The Interface Service.
 */
public abstract class ServiceBase implements Service {

    protected static final Logger LOGGER = LogManager.getLogger();
    private static final AtomicInteger SERVICE_ID_PROVIDER;

    static {
        SERVICE_ID_PROVIDER = new AtomicInteger(1000);
    }

    protected final LocalServerConnectionData serverConnectionData;
    protected final ServiceData serviceSpecifications;
    private final CountDownLatch serviceReadyState;

    {
        serviceReadyState = new CountDownLatch(1);
    }

    /**
     * Instantiates a new service base.
     *
     * @param serviceSpecifications the service specifications
     */
    protected ServiceBase(final ServiceData serviceSpecifications,
                          final LocalServerConnectionData serverConnectionData) {

        this.serviceSpecifications = serviceSpecifications;
        this.serverConnectionData = serverConnectionData;

        serviceSpecifications.setServiceId(SERVICE_ID_PROVIDER.getAndIncrement());
        serviceSpecifications.setServiceName(serviceSpecifications.getServiceBaseName() + "-"
                + serviceSpecifications.getServiceId() + "-" + serverConnectionData.getServerId());
    }

    @Override
    public final void waitForServiceReadiness() throws InterruptedException {
        serviceReadyState.await();
    }

    @Override
    public String getServiceName() {
        return serviceSpecifications.getServiceName();
    }

    @Override
    public TYPE getServiceType() {
        return serviceSpecifications.getServiceType();
    }

    @Override
    public void run() {
        setupThreadContext();
        LOGGER.info("{} started.", this.getServiceName());

        finishSetup();
        LOGGER.info("{} setup finished.", this.getServiceName());

        while (interruptionConditionNotMet()) {
            work();
        }

        LOGGER.info("{} termination initiated.", this.getServiceName());
        breakDown();
        LOGGER.info("{} terminated.", getServiceName());
        clearThreadContext();
    }

    protected void setupThreadContext() {
        final String serviceName = this.getServiceName();

        ThreadContext.put(LOG_ROUTE_CONTEXT_KEY, STANDARD_SERVER_ROUTE_VALUE);
        ThreadContext.put(LOG_FILE_CONTEXT_KEY, serviceName);
        Thread.currentThread().setName(serviceName);
    }

    /**
     * Used for final setup steps of services.
     */
    protected abstract void finishSetup();

    /**
     * Interruption condition for service.
     *
     * @return true, if is interruption condition met
     */
    public boolean interruptionConditionNotMet() {
        return !Thread.currentThread().isInterrupted();
    }

    /**
     * Contains services working steps.
     */
    protected abstract void work();

    /**
     * Used for essential steps during service's shutdown.
     */
    protected abstract void breakDown();

    protected void clearThreadContext() {
        ThreadContext.clearAll();
    }

    /**
     * Returns the service id.
     *
     * @return the service id
     */
    protected final int getServiceId() {
        return serviceSpecifications.getServiceId();
    }

    /**
     * Sets the ready state.
     */
    protected final void setReadyState() {
        this.serviceReadyState.countDown();
    }
}
