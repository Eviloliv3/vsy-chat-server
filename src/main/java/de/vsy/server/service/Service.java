
package de.vsy.server.service;

/**
 * Extends Runnable interface enabling users to identify different service types
 * and waiting certain conditions.
 */
public interface Service extends Runnable {

    /**
     * Waits for the service to reach a certain point of readiness.
     *
     * @throws InterruptedException if the current thread is interrupted while
     *                              waiting for the service to hit a certain point.
     */
    void waitForServiceReadiness() throws InterruptedException;

    /**
     * Returns the service name.
     *
     * @return the service name
     */
    String getServiceName();

    /**
     * Returns the service type.
     *
     * @return the service type
     */
    TYPE getServiceType();

    enum TYPE {
        CHAT_STATUS_UPDATE, ERROR_HANDLER, SERVER_TRANSFER, REQUEST_ROUTER
    }
}
