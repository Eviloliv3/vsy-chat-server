package de.vsy.server.persistent_data.server_data;

import static com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance;

import com.fasterxml.jackson.databind.JavaType;
import de.vsy.server.persistent_data.PersistenceDAO;
import de.vsy.server.persistent_data.PersistentDataFileCreator.DataFileDescriptor;
import de.vsy.shared_transmission.packet.Packet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The Class ClientTransactionDAO.
 *
 * @author Frederic Heath
 */
public class ClientTransactionDAO implements ServerDataAccess {

  private static final Logger LOGGER = LogManager.getLogger();
  private final PersistenceDAO dataProvider;

  /**
   * Instantiates a new client transaction DAO.*
   */
  public ClientTransactionDAO() {
    this.dataProvider = new PersistenceDAO(DataFileDescriptor.CLIENT_TRANSACTION, getDataFormat());
  }

  /**
   * Gets the dataManagement format.
   *
   * @return the dataManagement format
   */
  public static JavaType getDataFormat() {
    return defaultInstance().constructMapType(HashMap.class, String.class, Boolean.class);
  }

  /**
   * Adds the transaction.
   *
   * @param transactionHash the transaction hash
   * @return true, if successful
   */
  public boolean addTransaction(final String transactionHash) {
    var transactionAdded = false;

    if (transactionHash != null) {
      Map<String, Boolean> allTransactions;

      if (!this.dataProvider.acquireAccess(true)) {
        LOGGER.error("No exclusive write access.");
        return false;
      }
      allTransactions = readTransactions();
      transactionAdded = allTransactions.putIfAbsent(transactionHash, false) == null;

      if (transactionAdded) {
        this.dataProvider.writeData(allTransactions);
      }

      this.dataProvider.releaseAccess(true);
    }
    return transactionAdded;
  }

  /**
   * Read transactions.
   *
   * @return the map
   */
  @SuppressWarnings("unchecked")
  private Map<String, Boolean> readTransactions() {
    Map<String, Boolean> allTransactions = new HashMap<>();
    Object fromFile;

    if (!this.dataProvider.acquireAccess(false)) {
      LOGGER.error("No shared read access.");
      return allTransactions;
    }
    fromFile = this.dataProvider.readData();
    this.dataProvider.releaseAccess(false);

    try {
      allTransactions = (Map<String, Boolean>) fromFile;
    } catch (final ClassCastException cc) {
      LOGGER.info(
          "{} while reading the contact list. Empty list will be returned.",
          cc.getClass().getSimpleName());
    }
    return allTransactions;
  }

  /**
   * Complete transaction.
   *
   * @param transactionHash the transaction hash
   * @return true, if successful
   */
  public boolean completeTransaction(final String transactionHash) {
    var transactionComplete = false;

    if (transactionHash != null) {
      Map<String, Boolean> allTransactions;

      if (!this.dataProvider.acquireAccess(true)) {
        LOGGER.error("No exclusive write access.");
        return false;
      }
      allTransactions = readTransactions();

      if (allTransactions.containsKey(transactionHash)) {
        allTransactions.put(transactionHash, true);
        transactionComplete = this.dataProvider.writeData(allTransactions);
      }

      this.dataProvider.releaseAccess(true);
    }
    return transactionComplete;
  }

  @Override
  public void createFileAccess() throws InterruptedException {
    this.dataProvider.createFileReferences();
  }

  /**
   * Gets the all incomplete transactions.
   *
   * @return the all incomplete transactions
   */
  public Map<String, Boolean> getAllIncompleteTransactions() {

    final Map<String, Boolean> incompleteTransactions = new HashMap<>();

    if (!this.dataProvider.acquireAccess(false)) {
      LOGGER.error("No shared read access.");
      return incompleteTransactions;
    }
    final var allTransactions = readTransactions();
    this.dataProvider.releaseAccess(false);

    for (final var transaction : allTransactions.entrySet()) {
      final var completionState = transaction.getValue();

      if (completionState != null && !completionState) {
        incompleteTransactions.put(transaction.getKey(), false);
      }
    }
    return incompleteTransactions;
  }

  /**
   * Checks if is transaction completed.
   *
   * @param toCheck the to check
   * @return true, if is transaction completed
   */
  public boolean isTransactionCompleted(final Packet toCheck) {
    var transactionComplete = false;

    final var hashToCheck = toCheck.getPacketHash();
    Map<String, Boolean> readTransactions;

    if (hashToCheck != null) {

      if (!this.dataProvider.acquireAccess(false)) {
        LOGGER.error("No shared read access.");
        return false;
      }
      readTransactions = readTransactions();
      this.dataProvider.releaseAccess(false);
      transactionComplete = Objects.equals(true, readTransactions.get(hashToCheck));

    }

    return transactionComplete;
  }

  @Override
  public void removeFileAccess() {
    this.dataProvider.removeFileReferences();
  }
}
