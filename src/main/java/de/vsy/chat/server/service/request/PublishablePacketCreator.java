package de.vsy.chat.server.service.request;

import de.vsy.chat.shared_module.packet_exception.PacketProcessingException;
import de.vsy.chat.shared_transmission.packet.Packet;

public
interface PublishablePacketCreator {

    Packet createPublishablePacket (Packet input)
    throws PacketProcessingException;
}
