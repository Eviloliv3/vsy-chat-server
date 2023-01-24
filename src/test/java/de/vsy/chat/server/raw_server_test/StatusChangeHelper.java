package de.vsy.chat.server.raw_server_test;

import de.vsy.chat.server.server_test_helpers.ClientConnection;
import de.vsy.shared_transmission.packet.content.PacketContent;
import de.vsy.shared_transmission.packet.content.status.ClientStatusChangeDTO;
import de.vsy.shared_transmission.packet.content.status.MessengerSetupDTO;

import static de.vsy.chat.server.server_test_helpers.TestResponseSingleClient.checkResponse;
import static de.vsy.shared_transmission.packet.content.status.ClientService.MESSENGER;
import static de.vsy.shared_transmission.packet.property.communicator.CommunicationEndpoint.getServerEntity;
import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_SERVER_ID;

public class StatusChangeHelper {
    public static void changeStatus(ClientConnection connection, boolean changeTo) {
        PacketContent content = new ClientStatusChangeDTO(MESSENGER, changeTo,
                connection.getCommunicatorData());
        checkResponse(connection, getServerEntity(STANDARD_SERVER_ID), content,
                MessengerSetupDTO.class);
    }
}
