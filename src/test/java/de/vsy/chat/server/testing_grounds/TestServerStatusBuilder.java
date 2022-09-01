package de.vsy.chat.server.testing_grounds;

import de.vsy.server.server.client_management.ClientState;
import de.vsy.server.server_packet.content.builder.SimpleStatusSyncBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static de.vsy.shared_transmission.shared_transmission.dto.CommunicatorDTO.valueOf;
import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_CLIENT_ID;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestServerStatusBuilder {

    @Test
    public
    void createSimpleInternalContent () {
        var builder = new SimpleStatusSyncBuilder<>();
        var content = builder.withContactData(
                                     valueOf(STANDARD_CLIENT_ID, "testtest"))
                             .withClientState(ClientState.AUTHENTICATED)
                             .build();
    }
}
