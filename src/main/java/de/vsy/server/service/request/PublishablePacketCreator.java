package de.vsy.server.service.request;

import de.vsy.shared_module.shared_module.packet_exception.PacketProcessingException;
import de.vsy.shared_transmission.shared_transmission.packet.Packet;

public
interface PublishablePacketCreator {

    Packet handleDistributableContent (Packet input)
    throws PacketProcessingException;
}
