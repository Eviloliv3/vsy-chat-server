package de.vsy.server.client_handling.strategy;

import de.vsy.server.client_handling.data_management.HandlerLocalDataManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** The Class PendingChatPacketHandling. */
public
class PendingChatPacketHandling implements PacketHandlingStrategy {

    private static final Logger LOGGER = LogManager.getLogger();
    private final HandlerLocalDataManager handlerDataManager;

    /**
     * Instantiates a new pending chat PacketHandling.
     *
     * @param threadDataAccess the thread dataManagement accessLimiter
     */
    public
    PendingChatPacketHandling (final HandlerLocalDataManager threadDataAccess) {
        this.handlerDataManager = threadDataAccess;
    }

    @Override
    public
    void administerStrategy () {
        LOGGER.info("PendingClientHandler wird vorbereitet");
        Thread pendingClientThread;

        if (this.handlerDataManager.getGlobalAuthenticationStateControl()
                                   .changePendingState(true)) {

            pendingClientThread = new Thread(
                    new PendingClientBufferWatcher(this.handlerDataManager));
            pendingClientThread.start();
        } else {
            LOGGER.error("Global pending state could not be set for: {}",
                         this.handlerDataManager.getLocalClientDataProvider()
                                                .getClientData());
        }
    }
}
