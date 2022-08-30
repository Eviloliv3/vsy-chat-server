package de.vsy.server.exception_processing;

import de.vsy.server.server_packet.content.builder.SimpleInternalContentBuilder;
import de.vsy.shared_module.shared_module.packet_creation.PacketCompiler;
import de.vsy.shared_module.shared_module.packet_exception.PacketHandlingException;
import de.vsy.shared_module.shared_module.packet_exception.handler.BasicErrorResponseCreator;
import de.vsy.server.server.data.access.HandlerAccessManager;
import de.vsy.shared_transmission.shared_transmission.packet.Packet;

import java.util.HashSet;

public
class ServiceResponseCreator extends BasicErrorResponseCreator {

    @Override
    public
    Packet createErrorResponsePacket (PacketHandlingException phe,
                                      Packet toProcess) {
        final var contentWrapper = new SimpleInternalContentBuilder();
        final var errorData = super.createSimpleErrorData(phe, toProcess);
        final var content = contentWrapper.withContent(errorData)
                                          .withSyncedServers(new HashSet<>(
                                                  HandlerAccessManager.getLocalServerConnectionData()
                                                                      .getServerId()))
                                          .build();

        return PacketCompiler.createResponse(content, toProcess);
    }
}
