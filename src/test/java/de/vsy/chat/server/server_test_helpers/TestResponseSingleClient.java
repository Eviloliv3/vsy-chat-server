package de.vsy.chat.server.server_test_helpers;

import de.vsy.chat.shared_transmission.packet.Packet;
import de.vsy.chat.shared_transmission.packet.content.PacketContent;
import de.vsy.chat.shared_transmission.packet.content.error.ErrorDTO;
import de.vsy.chat.shared_transmission.packet.property.communicator.CommunicationEndpoint;
import org.junit.jupiter.api.Assertions;

public
class TestResponseSingleClient {

    /**
     * Sendet die definierte Anfrage und prüft, ob die empfangene Antwort vom
     * erwarteten Typ ist.
     *
     * @param clientOne Die zu verwendende Klientenverbindung
     * @param recipient Der Anfrageempfänger
     * @param request Der Inhalt der Anfrage
     * @param expectedResponseType Der erwartete Antworttyp
     */
    public static
    void checkResponse (ClientConnection clientOne, CommunicationEndpoint recipient,
                        PacketContent request,
                        Class<? extends PacketContent> expectedResponseType) {
        Packet packet;
        PacketContent content;

        clientOne.sendRequest(request, recipient);
        packet = clientOne.readPacket();

        if (packet != null) {
            content = packet.getPacketContent();
            Assertions.assertTrue(expectedResponseType.isInstance(content),
                                  () -> "Antworttyp ist \"" + content + "\"." +
                                        "\nErwartet wurde \"" +
                                        expectedResponseType.getSimpleName() + "\"");
        } else {
            Assertions.fail("Keine Antwort, an Stelle von \"" +
                            expectedResponseType.getSimpleName() + "\" erhalten.");
        }
    }

    /**
     * Sendet Anfrage und prüft Antwort auf speziellen Fehlercode/-string.
     *
     * @param clientOne Die zu verwendende Klientenverbindung
     * @param recipient Der Anfrageempfänger
     * @param request Der Inhalt der Anfrage
     * @param expectedErrorString Der erwartete Fehlercode/-string
     */
    public static
    void checkErrorResponse (ClientConnection clientOne,
                             CommunicationEndpoint recipient, PacketContent request,
                             String expectedErrorString) {
        Packet packet;
        PacketContent content;

        clientOne.sendRequest(request, recipient);
        packet = clientOne.readPacket();

        if (packet != null) {
            content = packet.getPacketContent();

            if (content instanceof ErrorDTO errorContent) {
                Assertions.assertTrue(
                        errorContent.getMessage().contains(expectedErrorString),
                        "Fehlermeldung enthaelt unerwartete Nachricht.\n" +
                        errorContent);
            } else {
                Assertions.fail(
                        "Antworttyp ist \"" + content.getClass().getSimpleName() +
                        "\"." + "\nErwartet wurde eine Fehlerantwort.");
            }
        } else {
            Assertions.fail(
                    "Keine Antwort, an Stelle einer Fehlerantwort erhalten.");
        }
    }
}
