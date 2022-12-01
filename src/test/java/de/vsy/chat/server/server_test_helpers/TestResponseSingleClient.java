package de.vsy.chat.server.server_test_helpers;

import de.vsy.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.packet.content.PacketContent;
import de.vsy.shared_transmission.packet.content.error.ErrorDTO;
import de.vsy.shared_transmission.packet.property.communicator.CommunicationEndpoint;
import org.junit.jupiter.api.Assertions;

public class TestResponseSingleClient {

  public static void checkResponse(ClientConnection clientOne, CommunicationEndpoint recipient,
      PacketContent request,
      Class<? extends PacketContent> expectedResponseType) {
    Packet packet;
    PacketContent content;

    clientOne.sendRequest(request, recipient);
    packet = clientOne.readPacket();

    if (packet != null) {
      content = packet.getPacketContent();
      Assertions.assertTrue(expectedResponseType.isInstance(content),
          () -> "Response type \"" + content + "\"."
              + "\nExpected type \"" + expectedResponseType.getSimpleName() + "\"");
    } else {
      Assertions.fail("No response instead of \"" + expectedResponseType.getSimpleName() + "\".");
    }
  }

  public static void checkErrorResponse(ClientConnection clientOne, CommunicationEndpoint recipient,
      PacketContent request, String expectedErrorString) {
    Packet packet;
    PacketContent content;

    clientOne.sendRequest(request, recipient);
    packet = clientOne.readPacket();

    if (packet != null) {
      content = packet.getPacketContent();

      if (content instanceof ErrorDTO errorContent) {
        Assertions.assertTrue(errorContent.getErrorMessage().contains(expectedErrorString),
            "Error notification contains unexpected message.\n" + errorContent);
      } else {
        Assertions.fail("Response type \"" + content.getClass().getSimpleName() + "\"."
            + "\nError response expected.");
      }
    } else {
      Assertions.fail("No response instead of error response.");
    }
  }
}
