package de.vsy.chat.server.server_test_helpers;

import de.vsy.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.packet.content.PacketContent;
import org.junit.jupiter.api.Assertions;

public class TestPacketVerifier {
    public static void verifyPacketContent(Packet packet, Class<? extends PacketContent> expectedType) {
        final PacketContent content;
        Assertions.assertNotNull(packet, "No response.");
        content = packet.getPacketContent();
        Assertions.assertInstanceOf(expectedType, content, () -> "Response type \"" + content + "\"."
                + "\nExpected type \"" + expectedType.getSimpleName() + "\"");
    }
}
