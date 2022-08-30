package de.vsy.server.server_packet.content;

import de.vsy.shared_transmission.shared_transmission.packet.content.PacketContent;

import java.io.Serial;

/**
 * Wird an den Stellvertreterthread gesandt, damit dieser seine Aktivitäten
 * einstellt.
 */
public
class ReconnectNotificationDTO implements PacketContent {

    @Serial
    private static final long serialVersionUID = 2770170836029862639L;
    private final boolean reconnectionState;

    /**
     * Instantiates a new reconnect notification dataManagement.
     *
     * @param reconnectionState the reconnection state
     */
    public
    ReconnectNotificationDTO (final boolean reconnectionState) {
        this.reconnectionState = reconnectionState;
    }

    /**
     * Gets the reconnection state.
     *
     * @return the reconnection state
     */
    public
    boolean getReconnectionState () {
        return this.reconnectionState;
    }
}
