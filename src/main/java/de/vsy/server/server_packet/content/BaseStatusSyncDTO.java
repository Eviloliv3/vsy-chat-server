package de.vsy.server.server_packet.content;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.vsy.server.client_management.ClientState;
import de.vsy.server.server_packet.content.builder.SimpleStatusSyncBuilder;
import de.vsy.shared_transmission.dto.CommunicatorDTO;

import java.io.Serial;

/**
 * Contains client's local state, that shall be transferred to other servers.
 */
@JsonDeserialize(builder = SimpleStatusSyncBuilder.class)
public class BaseStatusSyncDTO extends ServerPacketContentImpl {

    @Serial
    private static final long serialVersionUID = 5018800882273083846L;
    private final CommunicatorDTO contactData;
    private final ClientState clientState;
    private final boolean isToAdd;

    public BaseStatusSyncDTO(SimpleStatusSyncBuilder<? extends SimpleStatusSyncBuilder<?>> builder) {
        super(builder);
        this.contactData = builder.getCommunicatorData();
        this.clientState = builder.getClientState();
        this.isToAdd = builder.getIsToAdd();
    }

    /**
     * Returns the client state.
     *
     * @return ClientState
     */
    public ClientState getClientState() {
        return this.clientState;
    }

    /**
     * Returns the client id.
     *
     * @return CommunicatorDTO
     */
    public CommunicatorDTO getContactData() {
        return this.contactData;
    }

    @Override
    public String toString() {
        return "\"baseStatusSync\": {" + super.toString() + ", \"contactData\": " + this.contactData
                + ", "
                + "\"isToAdd\": " + this.isToAdd + ", \"clientState\": " + this.clientState + "}";
    }

    /**
     * Returns addition state.
     *
     * @return boolean
     */
    public boolean isToAdd() {
        return this.isToAdd;
    }
}
