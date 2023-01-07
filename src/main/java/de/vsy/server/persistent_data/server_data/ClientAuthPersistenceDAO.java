/*
 *
 */
package de.vsy.server.persistent_data.server_data;

import com.fasterxml.jackson.databind.JavaType;
import de.vsy.server.persistent_data.PersistenceDAO;
import de.vsy.server.persistent_data.PersistentDataFileCreator.DataFileDescriptor;
import de.vsy.server.persistent_data.data_bean.AuthenticationData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Set;

import static com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance;
import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_CLIENT_ID;

/**
 * Grants writing accessLimiter to a file containing all registered clients' account
 * dataManagement.
 */
public class ClientAuthPersistenceDAO implements ServerDataAccess {

    private static final Logger LOGGER = LogManager.getLogger();
    private final PersistenceDAO dataProvider;

    /**
     * Instantiates a new account modifier.
     */
    public ClientAuthPersistenceDAO() {

        this.dataProvider = new PersistenceDAO(DataFileDescriptor.REGISTERED_CLIENTS, getDataFormat());
    }

    /**
     * Returns the dataManagement format.
     *
     * @return the dataManagement format
     */
    public static JavaType getDataFormat() {
        return defaultInstance().constructCollectionType(HashSet.class, AuthenticationData.class);
    }

    /**
     * Check client id.
     *
     * @param clientId the client id
     * @return true, if successful
     */
    public boolean checkClientId(final int clientId) {
        var idFound = false;
        Set<AuthenticationData> regClients;

        if (!this.dataProvider.acquireAccess(false)) {
            LOGGER.error("No shared read access.");
            return false;
        }
        regClients = readRegisteredClients();
        this.dataProvider.releaseAccess(false);

        for (final AuthenticationData authData : regClients) {

            if (clientId == authData.getClientId()) {
                idFound = true;
                break;
            }
        }
        return idFound;
    }

    /**
     * Read registered clients.
     *
     * @return the list
     */
    @SuppressWarnings("unchecked")
    private Set<AuthenticationData> readRegisteredClients() {
        Object fromFile;
        Set<AuthenticationData> readList = null;

        if (!this.dataProvider.acquireAccess(false)) {
            LOGGER.error("No shared read access.");
            return new HashSet<>();
        }
        fromFile = this.dataProvider.readData();
        this.dataProvider.releaseAccess(false);

        if (fromFile instanceof HashSet) {

            try {
                readList = (HashSet<AuthenticationData>) fromFile;
            } catch (final ClassCastException cc) {
                LOGGER.info(
                        "{} occurred while reading the registered client map. Empty map will be returned.",
                        cc.getClass().getSimpleName());
            }
        }

        if (readList == null) {
            readList = new HashSet<>();
        }
        return readList;
    }

    @Override
    public void createFileAccess() throws InterruptedException {
        this.dataProvider.createFileReferences();
    }

    /**
     * Returns the client id.
     *
     * @param username the username
     * @param password the password
     * @return the client id
     */
    public int getClientId(final String username, final String password) {
        int clientId = STANDARD_CLIENT_ID;
        Set<AuthenticationData> readList;
        var clientAuth = AuthenticationData.valueOf(username, password, STANDARD_CLIENT_ID);

        if (!this.dataProvider.acquireAccess(false)) {
            LOGGER.error("No shared read access.");
            return clientId;
        }
        readList = readRegisteredClients();
        this.dataProvider.releaseAccess(false);

        for (final AuthenticationData authData : readList) {

            if (authData.sameCredentials(clientAuth)) {
                clientId = authData.getClientId();
                break;
            }
        }
        return clientId;
    }

    /**
     * Removes the account dataManagement.
     *
     * @param clientId the to delete
     * @return true, if successful
     */
    public boolean removeAccountData(final int clientId) {
        var accountRemoved = false;
        Set<AuthenticationData> regClients;

        if (!this.dataProvider.acquireAccess(true)) {
            LOGGER.error("No exclusive write access.");
            return false;
        }
        regClients = readRegisteredClients();

        for (final var authData : regClients) {

            if (authData.getClientId() == clientId) {
                accountRemoved = regClients.remove(authData) && this.dataProvider.writeData(regClients);
            }
        }
        this.dataProvider.releaseAccess(true);

        return accountRemoved;
    }

    @Override
    public void removeFileAccess() {
        this.dataProvider.removeFileReferences();
    }

    /**
     * Save account dataManagement.
     *
     * @param toAdd the to add
     * @return true, if successful
     */
    public boolean saveAccountData(final AuthenticationData toAdd) {
        var accountRegistered = false;
        var alreadyRegistered = false;
        Set<AuthenticationData> regClients;

        if (toAdd != null) {
            if (!this.dataProvider.acquireAccess(true)) {
                LOGGER.error("No exclusive write access.");
                return false;
            }
            regClients = readRegisteredClients();

            for (final AuthenticationData authData : regClients) {

                if (authData.sameLogin(toAdd)) {
                    alreadyRegistered = true;
                    break;
                }
            }

            if (!alreadyRegistered && regClients.add(toAdd) && this.dataProvider.writeData(regClients)) {
                accountRegistered = true;
                LOGGER.info("Account created.");
            }

            this.dataProvider.releaseAccess(true);
        }

        return accountRegistered;
    }
}
