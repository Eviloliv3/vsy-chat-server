package de.vsy.server.exception_processing;

import de.vsy.shared_module.shared_module.exception_processing.PacketHandlingExceptionCreator;
import de.vsy.shared_module.shared_module.exception_processing.PacketHandlingExceptionProcessor;
import de.vsy.shared_module.shared_module.packet_exception.handler.BasicAnswerabilityCheck;

public class ServerPacketHandlingExceptionCreator extends PacketHandlingExceptionCreator {

  private ServerPacketHandlingExceptionCreator() {
    super();
  }

  public static PacketHandlingExceptionProcessor getServiceExceptionProcessor() {
    return new PacketHandlingExceptionProcessor(BasicAnswerabilityCheck::checkPacketAnswerable,
        new ServiceResponseCreator());
  }
}
