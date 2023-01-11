package de.vsy.server.persistent_data.server_data;

import com.fasterxml.jackson.databind.JavaType;
import de.vsy.server.persistent_data.SynchronousFileManipulator;
import de.vsy.server.persistent_data.DataFileDescriptor;
import de.vsy.shared_transmission.packet.Packet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance;

/**
 * The Class ClientTransactionDAO.
 */
public class ClientTransactionDAO extends ServerDAO {


    /**
     * Instantiates a new client transaction DAO.*
     */
    public ClientTransactionDAO() {
        super(DataFileDescriptor.CLIENT_TRANSACTION, getDataFormat());
    }

    /**
     * Returns the dataManagement format.
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

            if (!super.dataProvider.acquireAccess(false)) {
                LOGGER.error("No exclusive write access.");
                return false;
            }
            allTransactions = readTransactions();
            transactionAdded = allTransactions.putIfAbsent(transactionHash, false) == null;

            if (transactionAdded) {
                super.dataProvider.writeData(allTransactions);
            }

            super.dataProvider.releaseAccess(false);
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

        if (!super.dataProvider.acquireAccess(true)) {
            LOGGER.error("No shared read access.");
            return allTransactions;
        }
        fromFile = super.dataProvider.readData();
        super.dataProvider.releaseAccess(true);

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

            if (!super.dataProvider.acquireAccess(false)) {
                LOGGER.error("No exclusive write access.");
                return false;
            }
            allTransactions = readTransactions();

            if (allTransactions.containsKey(transactionHash)) {
                allTransactions.put(transactionHash, true);
                transactionComplete = super.dataProvider.writeData(allTransactions);
            }

            super.dataProvider.releaseAccess(false);
        }
        return transactionComplete;
    }

    /**
     * Returns the all incomplete transactions.
     *
     * @return the all incomplete transactions
     */
    public Map<String, Boolean> getAllIncompleteTransactions() {

        final Map<String, Boolean> incompleteTransactions = new HashMap<>();

        if (!super.dataProvider.acquireAccess(true)) {
            LOGGER.error("No shared read access.");
            return incompleteTransactions;
        }
        final var allTransactions = readTransactions();
        super.dataProvider.releaseAccess(true);

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

            if (!super.dataProvider.acquireAccess(true)) {
                LOGGER.error("No shared read access.");
                return false;
            }
            readTransactions = readTransactions();
            super.dataProvider.releaseAccess(true);
            transactionComplete = Objects.equals(true, readTransactions.get(hashToCheck));

        }

        return transactionComplete;
    }
}
