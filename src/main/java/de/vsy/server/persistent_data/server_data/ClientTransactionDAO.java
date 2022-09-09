package de.vsy.server.persistent_data.server_data;

import com.fasterxml.jackson.databind.JavaType;
import de.vsy.server.persistent_data.PersistenceDAO;
import de.vsy.server.persistent_data.PersistentDataFileCreator.DataFileDescriptor;
import de.vsy.shared_transmission.shared_transmission.packet.Packet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance;

/**
 * The Class ClientTransactionDAO.
 *
 * @author Frederic Heath
 */
public
class ClientTransactionDAO implements ServerDataAccess {

    private static final Logger LOGGER = LogManager.getLogger();
    private final PersistenceDAO dataProvider;

    /** Instantiates a new client transaction DAO.* */
    public
    ClientTransactionDAO () {
        this.dataProvider = new PersistenceDAO(DataFileDescriptor.CLIENT_TRANSACTION,
                                               getDataFormat());
    }

    /**
     * Gets the dataManagement format.
     *
     * @return the dataManagement format
     */
    public static
    JavaType getDataFormat () {
        return defaultInstance().constructMapType(HashMap.class, String.class,
                                                  Boolean.class);
    }

    /**
     * Adds the transaction.
     *
     * @param transactionHash the transaction hash
     *
     * @return true, if successful
     */
    public
    boolean addTransaction (final String transactionHash) {
        var transactionAdded = false;
        final var lockAlreadyAcquired = this.dataProvider.checkForActiveLock();

        if (transactionHash != null) {
            Map<String, Boolean> allTransactions;

            if (!lockAlreadyAcquired) {
                if(this.dataProvider.acquireAccess(false))
                    return false;
            }
            allTransactions = readTransactions();
            transactionAdded =
                    allTransactions.putIfAbsent(transactionHash, false) == null;

            if (transactionAdded) {
                this.dataProvider.writeData(allTransactions);
            }

            if (!lockAlreadyAcquired) {
                this.dataProvider.releaseAccess();
            }
        }
        return transactionAdded;
    }

    /**
     * Read transactions.
     *
     * @return the map
     */
    @SuppressWarnings("unchecked")
    private
    Map<String, Boolean> readTransactions () {
        Map<String, Boolean> allTransactions = new HashMap<>();
        final var lockAlreadyAcquired = this.dataProvider.checkForActiveLock();
        Object fromFile;

        if (!lockAlreadyAcquired) {
            if(this.dataProvider.acquireAccess(true))
                return allTransactions;
        }
        fromFile = this.dataProvider.readData();

        if (!lockAlreadyAcquired) {
            this.dataProvider.releaseAccess();
        }

        try {
            allTransactions = (Map<String, Boolean>) fromFile;
        } catch (final ClassCastException cc) {
            LOGGER.info(
                    "ClassCastException beim Lesen der Freundesliste. Die Liste wird leer ausgegeben.");
        }
        return allTransactions;
    }

    /**
     * Complete transaction.
     *
     * @param transactionHash the transaction hash
     *
     * @return true, if successful
     */
    public
    boolean completeTransaction (final String transactionHash) {
        var transactionComplete = false;
        final var lockAlreadyAcquired = this.dataProvider.checkForActiveLock();

        if (transactionHash != null) {
            Map<String, Boolean> allTransactions;

            if (!lockAlreadyAcquired) {
                if(this.dataProvider.acquireAccess(false))
                    return false;
            }
            allTransactions = readTransactions();

            if (allTransactions.containsKey(transactionHash)) {
                allTransactions.put(transactionHash, true);
                transactionComplete = this.dataProvider.writeData(allTransactions);
            }

            if (!lockAlreadyAcquired) {
                this.dataProvider.releaseAccess();
            }
        }
        return transactionComplete;
    }

    @Override
    public
    void createFileAccess ()
    throws InterruptedException {
        this.dataProvider.createFileReferences();
    }

    /**
     * Gets the all incomplete transactions.
     *
     * @return the all incomplete transactions
     */
    public
    Map<String, Boolean> getAllIncompleteTransactions () {
        final var lockAlreadyAcquired = this.dataProvider.checkForActiveLock();
        final Map<String, Boolean> incompleteTransactions = new HashMap<>();

        if (!lockAlreadyAcquired) {
            if(this.dataProvider.acquireAccess(true))
                return incompleteTransactions;
        }
        final var allTransactions = readTransactions();

        for (final var transaction : allTransactions.entrySet()) {
            final var completionState = transaction.getValue();

            if (completionState != null && !completionState) {
                incompleteTransactions.put(transaction.getKey(), completionState);
            }
        }

        if (!lockAlreadyAcquired) {
            this.dataProvider.releaseAccess();
        }
        return incompleteTransactions;
    }

    /**
     * Checks if is transaction completed.
     *
     * @param toCheck the to check
     *
     * @return true, if is transaction completed
     */
    public
    boolean isTransactionCompleted (final Packet toCheck) {
        var transactionComplete = false;
        final var lockAlreadyAcquired = this.dataProvider.checkForActiveLock();
        final var hashToCheck = toCheck.getPacketHash();
        Map<String, Boolean> readTransactions;

        if (hashToCheck != null) {

            if (!lockAlreadyAcquired) {
                if(this.dataProvider.acquireAccess(true))
                    return false;
            }
            readTransactions = readTransactions();
            transactionComplete = Objects.equals(true,
                                                 readTransactions.get(hashToCheck));

            if (!lockAlreadyAcquired) {
                this.dataProvider.releaseAccess();
            }
        }

        return transactionComplete;
    }

    @Override
    public
    void removeFileAccess () {
        this.dataProvider.removeFileReferences();
    }
}
