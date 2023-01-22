package de.vsy.server.client_handling.data_management;

import de.vsy.server.persistent_data.client_data.PendingPacketDAO;
import de.vsy.server.server_packet.content.SimpleInternalContentWrapper;
import de.vsy.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.packet.content.PacketContent;
import de.vsy.shared_transmission.packet.content.chat.TextMessageDTO;
import de.vsy.shared_transmission.packet.content.notification.ErrorDTO;
import de.vsy.shared_transmission.packet.content.relation.ContactRelationResponseDTO;
import de.vsy.shared_transmission.packet.property.packet_category.PacketCategory;

import java.util.List;

import static de.vsy.server.persistent_data.client_data.PendingType.PROCESSOR_BOUND;
import static de.vsy.shared_transmission.packet.property.packet_category.PacketCategory.*;

public class ResponseRetainer {
    private static final List<PacketCategory> VALID_RESPONSE_CATEGORIES;
    private static final List<Class<? extends PacketContent>> VALID_RESPONSE_TYPES;

    static {
        VALID_RESPONSE_CATEGORIES = List.of(RELATION, CHAT, NOTIFICATION);
        VALID_RESPONSE_TYPES = List.of(ContactRelationResponseDTO.class, TextMessageDTO.class, ErrorDTO.class);
    }
    public static Packet retainIfResponse(final Packet toCheck){
        final var category = toCheck.getPacketProperties().getPacketIdentificationProvider().getPacketCategory();

        if(VALID_RESPONSE_CATEGORIES.contains(category)){
            PacketContent content = toCheck.getPacketContent();

            if(content instanceof final SimpleInternalContentWrapper wrapper){
                content = wrapper.getWrappedContent();
            }

            if(VALID_RESPONSE_TYPES.contains(content.getClass())){

                if(content instanceof final TextMessageDTO message){
                    if(!message.getReceptionState()){
                        retainPacket(toCheck);
                        return null;
                    }
                }else{
                    retainPacket(toCheck);
                    return null;
                }
            }
        }
        return toCheck;
    }

    private static void retainPacket(final Packet toRetain){
        final var recipientId = toRetain.getPacketProperties().getRecipient().getEntityId();
        final var pendingPacketRetainer = new PendingPacketDAO();
        pendingPacketRetainer.createAccess(String.valueOf(recipientId));
        pendingPacketRetainer.appendPendingPacket(PROCESSOR_BOUND, toRetain);
    }
}
