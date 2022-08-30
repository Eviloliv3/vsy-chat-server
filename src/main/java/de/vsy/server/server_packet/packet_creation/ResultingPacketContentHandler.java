package de.vsy.server.server_packet.packet_creation;

import de.vsy.server.client_handling.packet_processing.processor.ResultingPacketCreator;
import de.vsy.server.server_packet.dispatching.PacketTransmissionCache;
import de.vsy.shared_transmission.shared_transmission.packet.content.PacketContent;

public
class ResultingPacketContentHandler {

    private final PacketTransmissionCache packetCache;
    private final ResultingPacketCreator standardResultCreator;

    public
    ResultingPacketContentHandler (
            final ResultingPacketCreator standardResultCreator,
            final PacketTransmissionCache packetCache) {
        this.standardResultCreator = standardResultCreator;
        this.packetCache = packetCache;
    }

    public
    void addResponse (final PacketContent responseContent) {
        final var response = this.standardResultCreator.createResponse( responseContent);
        this.packetCache.addPacket(response);
    }

    public
    void addRequest (final PacketContent followUpContent) {
        final var response = this.standardResultCreator.createRequest(
                followUpContent);
        this.packetCache.addPacket(response);
    }

    public
    void setError (final PacketContent responseContent) {
        final var response = this.standardResultCreator.createResponse(
                responseContent);
        this.packetCache.putError(response);
    }
}
