/*
 *
 */
package de.vsy.chat.server.service;

/**
 * Used to identify Services fulfilling a specific portion of request Packet.
 */
public
interface Service extends Runnable {

    enum TYPE {
        CHAT_STATUS_UPDATE,
        ERROR_HANDLER,
        SERVER_TRANSFER,
        REQUEST_ROUTER
    }

    /**
     * Gets the ready state.
     *
     * @return the ready state
     */
    boolean getReadyState ();

    /**
     * Gets the service name.
     *
     * @return the service name
     */
    String getServiceName ();

    /**
     * Gets the service type.
     *
     * @return the service type
     */
    TYPE getServiceType ();
}
