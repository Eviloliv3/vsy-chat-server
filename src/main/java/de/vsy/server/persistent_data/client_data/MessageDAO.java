
package de.vsy.server.persistent_data.client_data;

import com.fasterxml.jackson.databind.JavaType;
import de.vsy.server.persistent_data.DataFileDescriptor;
import de.vsy.shared_transmission.packet.content.chat.TextMessageDTO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance;

/**
 * Grants writing accessLimiter to the file containing a client's message histories.
 */
public class MessageDAO extends ClientDAO {

    /**
     * Maximum amount of Messages that will be saved.
     */
    private static final int MAX_HISTORY_LENGTH = 50;

    /**
     * Instantiates a new message writer.
     */
    public MessageDAO() {
        super(DataFileDescriptor.MESSAGE_HISTORY, getDataFormat());
    }

    /**
     * Returns the dataManagement format.
     *
     * @return the dataManagement format
     */
    public static JavaType getDataFormat() {
        final var factory = defaultInstance();
        final JavaType msgListType = factory.constructCollectionType(ArrayList.class,
                TextMessageDTO.class);
        final var mapKeyType = factory.constructType(Integer.class);
        return factory.constructMapType(HashMap.class, mapKeyType, msgListType);
    }

    /**
     * Read client messages.
     *
     * @param clientId the client id
     * @return the linked list
     */
    public List<TextMessageDTO> readClientMessages(final int clientId) {
        Map<Integer, List<TextMessageDTO>> readMap;
        List<TextMessageDTO> readMessages;

        if (!super.dataProvider.acquireAccess(true)) {
            LOGGER.error("No shared read access.");
            return new ArrayList<>();
        }
        readMap = readAllClientMessages();
        super.dataProvider.releaseAccess(true);
        readMessages = readMap.get(clientId);

        if (readMessages == null) {
            readMessages = new ArrayList<>();
        }
        return readMessages;
    }

    /**
     * Read all client messages.
     *
     * @return the hash map
     */
    @SuppressWarnings("unchecked")
    private Map<Integer, List<TextMessageDTO>> readAllClientMessages() {
        var readMap = new HashMap<Integer, List<TextMessageDTO>>();
        Object fromFile;

        if (!super.dataProvider.acquireAccess(true)) {
            LOGGER.error("No shared read access.");
            return readMap;
        }
        fromFile = super.dataProvider.readData();
        super.dataProvider.releaseAccess(true);

        if (fromFile instanceof HashMap) {

            try {
                readMap = (HashMap<Integer, List<TextMessageDTO>>) fromFile;
            } catch (final ClassCastException cc) {
                LOGGER.info(
                        "{} occurred while reading the message map. Empty map will be returned.",
                        cc.getClass().getSimpleName());
            }
        }
        return readMap;
    }

    public void removeMessages(final int contactId) {
        Map<Integer, List<TextMessageDTO>> oldMessages;

        if (!super.dataProvider.acquireAccess(false)) {
            LOGGER.error("No exclusive write access.");
            return;
        }
        oldMessages = this.readAllClientMessages();
        oldMessages.remove(contactId);
        super.dataProvider.writeData(oldMessages);
        super.dataProvider.releaseAccess(false);
    }

    /**
     * Save message.
     *
     * @param contactId the contact id
     * @param msg       the msg
     * @return true, if successful
     */
    public boolean saveMessage(final int contactId, final TextMessageDTO msg) {
        var messageSaved = false;
        Map<Integer, List<TextMessageDTO>> oldMessages;
        List<TextMessageDTO> msgHistory;

        if (contactId > 0 && msg != null) {
            if (!super.dataProvider.acquireAccess(false)) {
                LOGGER.error("No exclusive write access.");
                return false;
            }
            oldMessages = readAllClientMessages();
            msgHistory = oldMessages.getOrDefault(contactId, new ArrayList<>());
            msgHistory.add(msg);

            while (msgHistory.size() >= MAX_HISTORY_LENGTH) {
                msgHistory.remove(0);
            }
            oldMessages.put(contactId, msgHistory);
            messageSaved = super.dataProvider.writeData(oldMessages);

            super.dataProvider.releaseAccess(false);
        }
        return messageSaved;
    }
}
