package de.vsy.server.service;

import static de.vsy.shared_utility.standard_value.ThreadContextValues.LOG_FILE_CONTEXT_KEY;
import static de.vsy.shared_utility.standard_value.ThreadContextValues.LOG_ROUTE_CONTEXT_KEY;
import static de.vsy.shared_utility.standard_value.ThreadContextValues.STANDARD_SERVER_ROUTE_VALUE;

import de.vsy.server.server.data.socketConnection.LocalServerConnectionData;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

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
  /**
   * Flag signalisiert dem Aufrufer des Service, dass dieser Einsatzbereit ist.
   */
  private final CountDownLatch serviceReadyState;

  {
    serviceReadyState = new CountDownLatch(1);
  }

  /**
   * Instantiates a new service base.
   *
   * @param serviceSpecifications the service specifications
   * @param serviceBuffers        the service buffers
   */
  protected ServiceBase(final ServiceData serviceSpecifications,
      final ServicePacketBufferManager serviceBuffers,
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
   * Schliesst die Einrichtung des Services ab.
   */
  protected abstract void finishSetup();

  /**
   * Abbruchbedingung für den Service.
   *
   * @return true, if is interruption condition met
   */
  public boolean interruptionConditionNotMet() {
    return !Thread.currentThread().isInterrupted();
  }

  /**
   * Arbeitslogik.
   */
  protected abstract void work();

  /**
   * Schliesst das Beenden eines Services ab.
   */
  protected abstract void breakDown();

  protected void clearThreadContext() {
    ThreadContext.clearAll();
  }

  /**
   * Gets the service id.
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
