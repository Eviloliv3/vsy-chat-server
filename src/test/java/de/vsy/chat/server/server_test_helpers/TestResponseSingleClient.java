package de.vsy.chat.server.server_test_helpers;

import de.vsy.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.packet.content.PacketContent;
import de.vsy.shared_transmission.packet.content.error.ErrorDTO;
import de.vsy.shared_transmission.packet.property.communicator.CommunicationEndpoint;
import org.junit.jupiter.api.Assertions;

import static de.vsy.chat.server.server_test_helpers.TestPacketVerifier.verifyPacketContent;

public class TestResponseSingleClient {

    public static void checkResponse(ClientConnection clientOne, CommunicationEndpoint recipient,
                                     PacketContent request,
                                     Class<? extends PacketContent> expectedResponseType) {
        Packet packet;
        clientOne.sendRequest(request, recipient);
        packet = clientOne.readPacket();
        verifyPacketContent(packet, expectedResponseType);
    }

    public static void checkErrorResponse(ClientConnection clientOne, CommunicationEndpoint recipient,
                                          PacketContent request, String expectedErrorString) {
        Packet packet;
        ErrorDTO errorContent;

        clientOne.sendRequest(request, recipient);
        packet = clientOne.readPacket();
        verifyPacketContent(packet, ErrorDTO.class);
        errorContent = (ErrorDTO) packet.getPacketContent();
        Assertions.assertTrue(errorContent.getErrorMessage().contains(expectedErrorString),
                "Error notification contains unexpected message.\n" + errorContent);
    }
}
