package de.vsy.server.server_packet.packet_validation;

import static de.vsy.shared_transmission.packet.property.packet_category.PacketCategory.AUTHENTICATION;
import static de.vsy.shared_transmission.packet.property.packet_category.PacketCategory.CHAT;
import static de.vsy.shared_transmission.packet.property.packet_category.PacketCategory.ERROR;
import static de.vsy.shared_transmission.packet.property.packet_category.PacketCategory.RELATION;
import static de.vsy.shared_transmission.packet.property.packet_category.PacketCategory.STATUS;
import static de.vsy.shared_transmission.packet.property.packet_type.ChatType.GROUP_MESSAGE;
import static de.vsy.shared_transmission.packet.property.packet_type.ChatType.GROUP_REGISTRATION;
import static de.vsy.shared_transmission.packet.property.packet_type.ChatType.TEXT_MESSAGE;
import static de.vsy.shared_transmission.packet.property.packet_type.ErrorType.SIMPLE_ERROR;
import static de.vsy.shared_transmission.packet.property.packet_type.RelationType.CONTACT_RELATION;
import static de.vsy.shared_transmission.packet.property.packet_type.StatusType.CHAT_STATUS;
import static java.util.Set.of;

import de.vsy.server.server_packet.content.BaseStatusSyncDTO;
import de.vsy.server.server_packet.content.ExtendedStatusSyncDTO;
import de.vsy.server.server_packet.content.InterServerCommSyncDTO;
import de.vsy.server.server_packet.content.ServerFailureDTO;
import de.vsy.server.server_packet.content.SimpleInternalContentWrapper;
import de.vsy.server.server_packet.packet_properties.packet_type.ServerErrorType;
import de.vsy.server.server_packet.packet_properties.packet_type.ServerStatusType;
import de.vsy.shared_module.packet_validation.SemanticPacketValidator;
import de.vsy.shared_transmission.packet.content.PacketContent;
import de.vsy.shared_transmission.packet.property.packet_category.PacketCategory;
import de.vsy.shared_transmission.packet.property.packet_type.AuthenticationType;
import de.vsy.shared_transmission.packet.property.packet_type.PacketType;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
   * @return Map<PacketCategory, Map<PacketType, Set<Class<? extends PacketContent>>>>
   */
  public static Map<PacketCategory, Map<PacketType, Set<Class<? extends PacketContent>>>> setupRegularServerPacketContentValidation() {
    Map<PacketCategory, Map<PacketType, Set<Class<? extends PacketContent>>>> packetValidator = new EnumMap<>(
        PacketCategory.class);

    packetValidator.put(STATUS, setupStatusValidation());
    packetValidator.put(ERROR, setupErrorValidation());
    packetValidator.put(AUTHENTICATION, setupAuthenticationValidation());
    packetValidator.put(RELATION, setupRelationValidation());
    packetValidator.put(CHAT, setupChatValidation());

    return packetValidator;
  }

  /**
   * Setup status validation.
   *
   * @return Map<PacketType, Set<Class<? extends PacketContent>>>
   */
  public static Map<PacketType, Set<Class<? extends PacketContent>>> setupStatusValidation() {
    Map<PacketType, Set<Class<? extends PacketContent>>> statusMapping = new HashMap<>();

    statusMapping.put(ServerStatusType.CLIENT_STATUS,
        of(BaseStatusSyncDTO.class, ExtendedStatusSyncDTO.class));
    statusMapping.put(ServerStatusType.SERVER_STATUS, of(InterServerCommSyncDTO.class));
    statusMapping.put(CHAT_STATUS, of(SimpleInternalContentWrapper.class));
    return statusMapping;
  }

  /**
   * Setup error validation.
   *
   * @return Map<PacketType, Set<Class<? extends PacketContent>>>
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

    authMapping.put(AuthenticationType.CLIENT_LOGIN, of(SimpleInternalContentWrapper.class));
    authMapping.put(AuthenticationType.CLIENT_LOGOUT, of(SimpleInternalContentWrapper.class));
    authMapping.put(AuthenticationType.CLIENT_NEW_ACCOUNT, of(SimpleInternalContentWrapper.class));
    authMapping.put(AuthenticationType.CLIENT_RECONNECT, of(SimpleInternalContentWrapper.class));

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
