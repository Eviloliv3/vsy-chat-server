/*
 *
 */
package de.vsy.server.persistent_data.client_data;

import static com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance;
import static java.lang.String.valueOf;

import com.fasterxml.jackson.databind.JavaType;
import de.vsy.server.persistent_data.PersistenceDAO;
import de.vsy.server.persistent_data.PersistentDataFileCreator.DataFileDescriptor;
import de.vsy.shared_transmission.shared_transmission.packet.content.chat.TextMessageDTO;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Grants writing accessLimiter to the file containing a client's message histories.
 */
public class MessageDAO implements ClientDataAccess {

  /**
   * Die maximale Anzahl daürhaft gespeicherter Nachrichten je Gespräch.
   */
  private static final int MAX_HISTORY_LENGTH = 50;
  private static final Logger LOGGER = LogManager.getLogger();
  private final PersistenceDAO dataProvider;

  /**
   * Instantiates a new message writer.
   */
  public MessageDAO() {

    this.dataProvider = new PersistenceDAO(DataFileDescriptor.MESSAGE_HISTORY, getDataFormat());
  }

  /**
   * Gets the dataManagement format.
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

  @Override
  public void createFileAccess(final int clientId) throws InterruptedException {
    this.dataProvider.createFileReferences(valueOf(clientId));
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

    if (!this.dataProvider.acquireAccess(false)) {
      LOGGER.error("No shared read access.");
      return new ArrayList<>();
    }
    readMap = readAllClientMessages();
    this.dataProvider.releaseAccess(false);
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

    if (!this.dataProvider.acquireAccess(false)) {
      LOGGER.error("No shared read access.");
      return readMap;
    }
    fromFile = this.dataProvider.readData();
    this.dataProvider.releaseAccess(false);

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

    if (!this.dataProvider.acquireAccess(true)) {
      LOGGER.error("No exclusive write access.");
      return;
    }
    oldMessages = this.readAllClientMessages();
    oldMessages.remove(contactId);
    this.dataProvider.writeData(oldMessages);
    this.dataProvider.releaseAccess(true);
  }

  @Override
  public void removeFileAccess() {
    this.dataProvider.removeFileReferences();
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
      if (!this.dataProvider.acquireAccess(true)) {
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
      messageSaved = this.dataProvider.writeData(oldMessages);

      this.dataProvider.releaseAccess(true);
    }
    return messageSaved;
  }
}
