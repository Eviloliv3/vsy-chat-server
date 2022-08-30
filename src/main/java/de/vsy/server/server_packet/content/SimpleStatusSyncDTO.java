package de.vsy.server.server_packet.content;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.vsy.server.server.client_management.ClientState;
import de.vsy.server.server_packet.content.builder.SimpleStatusSyncBuilder;
import de.vsy.shared_transmission.shared_transmission.dto.CommunicatorDTO;

import java.io.Serial;

/**
 * Wird gesandt, wenn ein zusätzlicher Server über einen Zustandswechsel eines
 * Klienten informiert werden muss.
 */
@JsonDeserialize(builder = SimpleStatusSyncBuilder.class)
public
class SimpleStatusSyncDTO extends ServerPacketContentImpl {

    @Serial
    private static final long serialVersionUID = 5018800882273083846L;
    private final CommunicatorDTO contactData;
    private final ClientState clientState;
    private final boolean isToAdd;

    public
    SimpleStatusSyncDTO (
            SimpleStatusSyncBuilder<? extends SimpleStatusSyncBuilder<?>> builder) {
        super(builder);
        this.contactData = builder.getCommunicatorData();
        this.clientState = builder.getClientState();
        this.isToAdd = builder.getIsToAdd();
    }

    /**
     * Gets the client state.
     *
     * @return the client state
     */
    public
    ClientState getClientState () {
        return this.clientState;
    }

    /**
     * Gets the entity id.
     *
     * @return the entity id
     */
    public
    CommunicatorDTO getContactData () {
        return this.contactData;
    }

    @Override
    public
    String toString () {
        return "\"simpleStatusSync\": {" + super.toString() + ", \"contactData\": " +
               this.contactData + ", " + "\"isToAdd\": " + this.isToAdd +
               ", \"clientState\": " + this.clientState + "}";
    }

    /**
     * Checks if is to add.
     *
     * @return true, if is to add
     */
    public
    boolean isToAdd () {
        return this.isToAdd;
    }
}
