package de.vsy.chat.server.exception_processing;

import de.vsy.chat.shared_module.exception_processing.PacketHandlingExceptionCreator;
import de.vsy.chat.shared_module.exception_processing.PacketHandlingExceptionProcessor;
import de.vsy.chat.shared_module.packet_exception.handler.BasicAnswerabilityCheck;

public
class ServerPacketHandlingExceptionCreator extends PacketHandlingExceptionCreator {

    private
    ServerPacketHandlingExceptionCreator () {
        super();
    }

    public static
    PacketHandlingExceptionProcessor getServiceExceptionProcessor () {
        return new PacketHandlingExceptionProcessor(
                BasicAnswerabilityCheck::checkPacketAnswerable,
                new ServiceResponseCreator());
    }
}
