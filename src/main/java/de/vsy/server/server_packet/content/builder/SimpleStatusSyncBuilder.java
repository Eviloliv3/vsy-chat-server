package de.vsy.server.server_packet.content.builder;

import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import de.vsy.server.server.client_management.ClientState;
import de.vsy.server.server_packet.content.SimpleStatusSyncDTO;
import de.vsy.shared_module.shared_module.data_element_validation.BeanChecker;
import de.vsy.shared_transmission.shared_transmission.dto.CommunicatorDTO;

/**
 * The Class SimpleStatusSyncBuilder.
 *
 * @author Frederic Heath
 */
@JsonPOJOBuilder
public
class SimpleStatusSyncBuilder<T extends SimpleStatusSyncBuilder<T>>
        extends ServerPacketContentBuilder<T> {

    private CommunicatorDTO communicatorData = null;
    private ClientState clientState = null;
    private boolean idToAdd = false;

    public
    CommunicatorDTO getCommunicatorData () {
        return this.communicatorData;
    }

    public
    ClientState getClientState () {
        return this.clientState;
    }

    public
    boolean getIsToAdd () {
        return this.idToAdd;
    }

    /**
     * With.
     *
     * @param idToAdd the is to add
     *
     * @return the simple status sync builder
     */
    public
    SimpleStatusSyncBuilder<T> withToAdd (final boolean idToAdd) {
        this.idToAdd = idToAdd;
        return getInstanciable();
    }

    @Override
    public
    SimpleStatusSyncBuilder<T> getInstanciable () {
        return this;
    }

    @Override
    public
    SimpleStatusSyncDTO build () {
        return new SimpleStatusSyncDTO(this);
    }

    /**
     * With.
     *
     * @param clientState the client state
     *
     * @return the simple status sync builder
     */
    public
    SimpleStatusSyncBuilder<T> withClientState (final ClientState clientState) {
        if (clientState == null) {
            throw new IllegalArgumentException("Kein Klientenzustand angegeben.");
        }
        this.clientState = clientState;
        return getInstanciable();
    }

    /**
     * With.
     *
     * @param communicatorData the client id
     *
     * @return the simple status sync builder
     */
    public
    SimpleStatusSyncBuilder<T> withContactData (
            final CommunicatorDTO communicatorData) {
        var communicatorDataCheck = BeanChecker.checkBean(communicatorData);

        if (communicatorDataCheck.isPresent()) {
            throw new IllegalArgumentException(communicatorDataCheck.get());
        }
        this.communicatorData = communicatorData;
        return getInstanciable();
    }
}
