package de.vsy.server.service;

import de.vsy.server.server.server_connection.LocalServerConnectionData;
import de.vsy.server.server_packet.dispatching.PacketDispatcher;
import de.vsy.server.service.packet_logic.PacketResponseMap;
import de.vsy.shared_transmission.shared_transmission.packet.Packet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.util.concurrent.atomic.AtomicInteger;

import static de.vsy.shared_utility.standard_value.ThreadContextValues.*;
import static java.lang.String.format;

/** The Interface Service. */
public abstract
class ServiceBase implements Service {

    private static final AtomicInteger SERVICE_ID_PROVIDER;
    protected static final Logger LOGGER = LogManager.getLogger();
    protected final LocalServerConnectionData serverConnectionData;
    protected final ServiceData serviceSpecifications;
    /**
     * Flag signalisiert dem Aufrufer des Service, dass dieser Einsatzbereit ist.
     */
    private volatile boolean serviceReadyState = false;

    static {
        SERVICE_ID_PROVIDER = new AtomicInteger(1000);
    }

    /**
     * Instantiates a new service base.
     *
     * @param serviceSpecifications the service specifications
     * @param serviceBuffers the service buffers
     */
    protected
    ServiceBase (final ServiceData serviceSpecifications,
                 final ServicePacketBufferManager serviceBuffers,
                 final LocalServerConnectionData serverConnectionData) {

        this.serviceSpecifications = serviceSpecifications;
        this.serverConnectionData = serverConnectionData;

        serviceSpecifications.setServiceId(SERVICE_ID_PROVIDER.getAndIncrement());
        serviceSpecifications.setServiceName(serviceSpecifications.getServiceBaseName()
                                             + "-" + serviceSpecifications.getServiceId()
                                             + "-" + serverConnectionData.getServerId());
    }

    @Override
    public final
    boolean getReadyState () {
        return serviceReadyState;
    }

    @Override
    public
    String getServiceName () {
        return serviceSpecifications.getServiceName();
    }

    @Override
    public
    TYPE getServiceType () {
        return serviceSpecifications.getServiceType();
    }

    @Override
    public
    void run () {
        this.setupThreadContext();
        LOGGER.info("{} gestartet.", this.getServiceName());

        finishSetup();

        while (interruptionConditionNotMet()) {
            work();
        }

        breakDown();
        this.clearThreadContext();
        LOGGER.info("{} beendet.", getServiceName());
    }

    protected
    void setupThreadContext(){
        final String serviceName = this.getServiceName();

        ThreadContext.put(LOG_ROUTE_CONTEXT_KEY, STANDARD_SERVER_ROUTE_VALUE);
        ThreadContext.put(LOG_FILE_CONTEXT_KEY, serviceName);
        Thread.currentThread().setName(serviceName);
    }

    protected
    void clearThreadContext(){
        ThreadContext.clearAll();
    }

    /** Schliesst die Einrichtung des Services ab. */
    protected abstract
    void finishSetup ();

    /**
     * Abbruchbedingung für den Service.
     *
     * @return true, if is interruption condition met
     */
    public
    boolean interruptionConditionNotMet () {
        return !Thread.currentThread().isInterrupted();
    }

    /** Arbeitslogik. */
    protected abstract
    void work ();

    /** Schliesst das Beenden eines Services ab. */
    protected abstract
    void breakDown ();

    /**
     * Gets the service id.
     *
     * @return the service id
     */
    protected final
    int getServiceId () {
        return serviceSpecifications.getServiceId();
    }

    /** Sets the ready state. */
    protected final
    void setReadyState () {
        this.serviceReadyState = true;
    }
}
