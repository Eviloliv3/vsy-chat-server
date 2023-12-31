package de.vsy.server.server_packet.packet_validation;

import de.vsy.server.server_packet.content.*;
import de.vsy.server.server_packet.packet_properties.packet_type.ServerErrorType;
import de.vsy.shared_module.packet_validation.SemanticPacketValidator;
import de.vsy.shared_transmission.packet.content.PacketContent;
import de.vsy.shared_transmission.packet.property.packet_category.PacketCategory;
import de.vsy.shared_transmission.packet.property.packet_type.PacketType;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static de.vsy.server.server_packet.packet_properties.packet_type.ServerStatusType.CLIENT_STATUS;
import static de.vsy.server.server_packet.packet_properties.packet_type.ServerStatusType.SERVER_STATUS;
import static de.vsy.shared_transmission.packet.property.packet_category.PacketCategory.*;
import static de.vsy.shared_transmission.packet.property.packet_type.AuthenticationType.*;
import static de.vsy.shared_transmission.packet.property.packet_type.ChatType.*;
import static de.vsy.shared_transmission.packet.property.packet_type.ErrorType.SIMPLE_ERROR;
import static de.vsy.shared_transmission.packet.property.packet_type.RelationType.CONTACT_RELATION;
import static de.vsy.shared_transmission.packet.property.packet_type.StatusType.CHAT_STATUS;
import static java.util.Set.of;

/**
 * The Class ServerPacketContentTypeValidationCreator.
 */
public class ServerPermittedCategoryContentAssociationProvider {

    private ServerPermittedCategoryContentAssociationProvider() {
    }

    /**
     * Setup regular server PacketContent validator.
     *
     * @return the server PacketContent type validator
     */
    public static SemanticPacketValidator createRegularServerPacketContentValidator() {
        SemanticPacketValidator semanticValidator = new SemanticPacketValidator();
        var regularContentValidator = setupRegularServerPacketContentValidation();

        for (var categoryValidation : regularContentValidator.entrySet()) {
            semanticValidator.addCategoryAssociations(categoryValidation.getKey(),
                    categoryValidation.getValue());
        }
        return semanticValidator;
    }

    /**
     * Setup regular server PacketContent validator.
     *
     * @return Map<PacketCategory, Map < PacketType, Set < Class < ? extends PacketContent>>>>
     */
    public static Map<PacketCategory, Map<PacketType, Set<Class<? extends PacketContent>>>> setupRegularServerPacketContentValidation() {
        Map<PacketCategory, Map<PacketType, Set<Class<? extends PacketContent>>>> packetValidator = new EnumMap<>(
                PacketCategory.class);

        packetValidator.put(STATUS, setupStatusValidation());
        packetValidator.put(NOTIFICATION, setupErrorValidation());
        packetValidator.put(AUTHENTICATION, setupAuthenticationValidation());
        packetValidator.put(RELATION, setupRelationValidation());
        packetValidator.put(CHAT, setupChatValidation());

        return packetValidator;
    }

    /**
     * Setup status validation.
     *
     * @return Map<PacketType, Set < Class < ? extends PacketContent>>>
     */
    public static Map<PacketType, Set<Class<? extends PacketContent>>> setupStatusValidation() {
        Map<PacketType, Set<Class<? extends PacketContent>>> statusMapping = new HashMap<>();

        statusMapping.put(CLIENT_STATUS,
                of(BaseStatusSyncDTO.class, ExtendedStatusSyncDTO.class));
        statusMapping.put(SERVER_STATUS, of(InterServerCommSyncDTO.class));
        statusMapping.put(CHAT_STATUS, of(SimpleInternalContentWrapper.class));
        return statusMapping;
    }

    /**
     * Setup error validation.
     *
     * @return Map<PacketType, Set < Class < ? extends PacketContent>>>
     */
    public static Map<PacketType, Set<Class<? extends PacketContent>>> setupErrorValidation() {
        Map<PacketType, Set<Class<? extends PacketContent>>> errorMapping = new HashMap<>();

        errorMapping.put(ServerErrorType.SERVER_STATUS,
                of(ServerFailureDTO.class, SimpleInternalContentWrapper.class));
        errorMapping.put(SIMPLE_ERROR, of(SimpleInternalContentWrapper.class));
        return errorMapping;
    }

    private static Map<PacketType, Set<Class<? extends PacketContent>>> setupAuthenticationValidation() {
        Map<PacketType, Set<Class<? extends PacketContent>>> authMapping = new HashMap<>();

        authMapping.put(CLIENT_LOGIN, of(SimpleInternalContentWrapper.class));
        authMapping.put(CLIENT_LOGOUT, of(SimpleInternalContentWrapper.class));
        authMapping.put(CLIENT_ACCOUNT_CREATION, of(SimpleInternalContentWrapper.class));
        authMapping.put(CLIENT_RECONNECT, of(SimpleInternalContentWrapper.class));
        authMapping.put(CLIENT_ACCOUNT_DELETION, of(SimpleInternalContentWrapper.class));

        return authMapping;
    }

    private static Map<PacketType, Set<Class<? extends PacketContent>>> setupRelationValidation() {
        Map<PacketType, Set<Class<? extends PacketContent>>> relationMapping = new HashMap<>();

        relationMapping.put(CONTACT_RELATION, of(SimpleInternalContentWrapper.class));

        return relationMapping;
    }

    private static Map<PacketType, Set<Class<? extends PacketContent>>> setupChatValidation() {
        Map<PacketType, Set<Class<? extends PacketContent>>> chatMapping = new HashMap<>();

        chatMapping.put(TEXT_MESSAGE, of(SimpleInternalContentWrapper.class));
        chatMapping.put(GROUP_MESSAGE, of(SimpleInternalContentWrapper.class));
        chatMapping.put(GROUP_REGISTRATION, of(SimpleInternalContentWrapper.class));

        return chatMapping;
    }
}
