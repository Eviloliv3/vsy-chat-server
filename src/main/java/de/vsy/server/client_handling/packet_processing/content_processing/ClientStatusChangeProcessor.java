package de.vsy.server.client_handling.packet_processing.content_processing;

import de.vsy.server.client_handling.data_management.CommunicationEntityDataProvider;
import de.vsy.server.client_handling.data_management.StatusHandlingDataProvider;
import de.vsy.server.client_handling.data_management.logic.ClientStateControl;
import de.vsy.server.persistent_data.client_data.ContactListDAO;
import de.vsy.server.persistent_data.client_data.MessageDAO;
import de.vsy.server.server_packet.packet_creation.ResultingPacketContentHandler;
import de.vsy.shared_module.packet_exception.PacketProcessingException;
import de.vsy.shared_module.packet_processing.ContentProcessor;
import de.vsy.shared_transmission.dto.CommunicatorDTO;
import de.vsy.shared_transmission.packet.content.PacketContent;
import de.vsy.shared_transmission.packet.content.chat.TextMessageDTO;
import de.vsy.shared_transmission.packet.content.relation.EligibleContactEntity;
import de.vsy.shared_transmission.packet.content.status.ClientStatusChangeDTO;
import de.vsy.shared_transmission.packet.content.status.MessengerSetupDTO;
import de.vsy.shared_transmission.packet.content.status.MessengerTearDownDTO;

import java.util.*;

import static de.vsy.server.client_management.ClientState.ACTIVE_MESSENGER;

public class ClientStatusChangeProcessor implements ContentProcessor<ClientStatusChangeDTO> {

    private final ClientStateControl clientStateManager;
    private final CommunicationEntityDataProvider contactMapper;
    private final ContactListDAO contactProvider;
    private final MessageDAO messageReader;
    private final ResultingPacketContentHandler contentHandler;

    /**
     * Instantiates a new status change handler.
     *
     * @param threadDataAccess the thread dataManagement accessLimiter
     */
    public ClientStatusChangeProcessor(final StatusHandlingDataProvider threadDataAccess) {
        this.clientStateManager = threadDataAccess.getClientStateControl();
        this.contactMapper = threadDataAccess.getContactToActiveClientMapper();
        this.contactProvider = threadDataAccess.getLocalClientStateObserverManager()
                .getClientPersistentAccess()
                .getContactListDAO();
        this.messageReader = threadDataAccess.getLocalClientStateObserverManager()
                .getClientPersistentAccess()
                .getMessageDAO();
        this.contentHandler = threadDataAccess.getResultingPacketContentHandler();
    }

    @Override
    public void processContent(ClientStatusChangeDTO validatedContent)
            throws PacketProcessingException {
        String causeMessage = null;
        final var changeTo = validatedContent.getOnlineStatus();

        this.clientStateManager.changeLocalClientState(ACTIVE_MESSENGER, changeTo);

        if (this.clientStateManager.changePersistentClientState(ACTIVE_MESSENGER, changeTo)) {
            final PacketContent responseContent;

            if (changeTo) {
                this.clientStateManager.appendStateSynchronizationPacket(ACTIVE_MESSENGER, true);
                responseContent = prepareMessengerSetupDTO();
            } else {
                this.clientStateManager.appendStateSynchronizationPacket(ACTIVE_MESSENGER, false);
                responseContent = new MessengerTearDownDTO(true);
            }
            this.contentHandler.addResponse(responseContent);
        } else {
            this.clientStateManager.changeLocalClientState(ACTIVE_MESSENGER, false);
            causeMessage = "An error occurred while writing your global Messenger state. Please "
                    + "contact the ChatServer support team.";
        }

        if (causeMessage != null) {
            throw new PacketProcessingException(causeMessage);
        }
    }

    private MessengerSetupDTO prepareMessengerSetupDTO() {
        final var activeContactIdMap = this.getContactIdMap();
        final var activeContactMap = this.getActiveContactMap(activeContactIdMap);
        final var oldMessageMap = new HashMap<Integer, List<TextMessageDTO>>();

        for (var contactsEntry : activeContactIdMap.entrySet()) {
            for (var contactId : contactsEntry.getValue()) {
                var messageHistory = this.messageReader.readClientMessages(contactId);
                oldMessageMap.put(contactId, messageHistory);
            }
        }
        return new MessengerSetupDTO(oldMessageMap, activeContactMap);
    }

    private Map<EligibleContactEntity, Set<Integer>> getContactIdMap() {
        var contactIdMap = this.contactProvider.readContactMap();
        var activeClientIdSet = contactIdMap.get(EligibleContactEntity.CLIENT);

        if (activeClientIdSet != null) {
            activeClientIdSet = this.contactMapper.removeOfflineContacts(activeClientIdSet);
            contactIdMap.put(EligibleContactEntity.CLIENT, activeClientIdSet);
        }
        return contactIdMap;
    }

    private Map<EligibleContactEntity, Set<CommunicatorDTO>> getActiveContactMap(
            Map<EligibleContactEntity, Set<Integer>> activeContactIdMap) {
        var activeContactDataMap = new EnumMap<EligibleContactEntity, Set<CommunicatorDTO>>(
                EligibleContactEntity.class);

        for (var contactsEntry : activeContactIdMap.entrySet()) {
            var contactDataSet = this.contactMapper.mapToContactData(contactsEntry.getValue());
            activeContactDataMap.put(contactsEntry.getKey(), contactDataSet);
        }
        return activeContactDataMap;
    }
}
