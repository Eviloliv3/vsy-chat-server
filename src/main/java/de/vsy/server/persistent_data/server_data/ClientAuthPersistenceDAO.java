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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance;
import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_CLIENT_ID;

/**
 * Grants writing accessLimiter to a file containing all registered clients' account
 * dataManagement.
 */
public
class ClientAuthPersistenceDAO implements ServerDataAccess {

    private static final Logger LOGGER = LogManager.getLogger();
    private final PersistenceDAO dataProvider;

    /** Instantiates a new account modifier. */
    public
    ClientAuthPersistenceDAO () {

        this.dataProvider = new PersistenceDAO(DataFileDescriptor.REGISTERED_CLIENTS,
                                               getDataFormat());
    }

    /**
     * Gets the dataManagement format.
     *
     * @return the dataManagement format
     */
    public static
    JavaType getDataFormat () {
        return defaultInstance().constructCollectionType(HashSet.class,
                                                         AuthenticationData.class);
    }

    /**
     * Check client id.
     *
     * @param clientId the client id
     *
     * @return true, if successful
     */
    public
    boolean checkClientId (final int clientId) {
        var idFound = false;
        Set<AuthenticationData> regClients;

        final var lockAlreadyAcquired = this.dataProvider.checkForActiveLock();

        if (!lockAlreadyAcquired) {
            if(!this.dataProvider.acquireAccess(false))
                return false;
        }
        regClients = readRegisteredClients();

        for (final AuthenticationData authData : regClients) {

            if (clientId == authData.getClientId()) {
                idFound = true;
                break;
            }
        }
        if (!lockAlreadyAcquired)
            this.dataProvider.releaseAccess();
        return idFound;
    }

    /**
     * Read registered clients.
     *
     * @return the list
     */
    @SuppressWarnings("unchecked")
    private
    Set<AuthenticationData> readRegisteredClients () {
        Object fromFile;
        Set<AuthenticationData> readList = null;

        final var lockAlreadyAcquired = this.dataProvider.checkForActiveLock();

        if (!lockAlreadyAcquired) {
            if(this.dataProvider.acquireAccess(false))
                return new HashSet<>();
        }

        fromFile = this.dataProvider.readData();

        if (!lockAlreadyAcquired) {
            this.dataProvider.releaseAccess();
        }

        if (fromFile instanceof HashSet) {

            try {
                readList = (HashSet<AuthenticationData>) fromFile;
            } catch (final ClassCastException cc) {
                LOGGER.info(
                        "ClassCastException beim Lesen der registrierten Clients-" +
                        "Map. Die Map wird leer ausgegeben.");
            }
        }

        if (readList == null) {
            readList = new HashSet<>();
        }
        return readList;
    }

    @Override
    public
    void createFileAccess ()
    throws InterruptedException {
        this.dataProvider.createFileReferences();
    }

    /**
     * Gets the client id.
     *
     * @param loginName the login name
     * @param password the password
     *
     * @return the client id
     */
    public
    int getClientId (final String loginName, final String password) {
        int clientId = STANDARD_CLIENT_ID;
        Set<AuthenticationData> readList;
        var clientAuth = AuthenticationData.valueOf(loginName, password,
                                                    STANDARD_CLIENT_ID);

        final var lockAlreadyAcquired = this.dataProvider.checkForActiveLock();

        if (!lockAlreadyAcquired) {
            if(!this.dataProvider.acquireAccess(false))
                return clientId;
        }
        readList = readRegisteredClients();

        for (final AuthenticationData authData : readList) {

            if (authData.sameCredentials(clientAuth)) {
                clientId = authData.getClientId();
                break;
            }
        }

        if (!lockAlreadyAcquired) {
            this.dataProvider.releaseAccess();
        }

        return clientId;
    }

    /**
     * Removes the account dataManagement.
     *
     * @param clientId the to delete
     *
     * @return true, if successful
     */
    public
    boolean removeAccountData (final int clientId) {
        var accountRemoved = false;
        Set<AuthenticationData> regClients;

        final var lockAlreadyAcquired = this.dataProvider.checkForActiveLock();

        if (!lockAlreadyAcquired) {
            if(!this.dataProvider.acquireAccess(true))
                return false;
        }
        regClients = readRegisteredClients();

        for (final var authData : regClients) {

            if (authData.getClientId() == clientId) {
                accountRemoved = regClients.remove(authData) &&
                                 this.dataProvider.writeData(regClients);
            }
        }
        if (!lockAlreadyAcquired) {
            this.dataProvider.releaseAccess();
        }

        return accountRemoved;
    }

    @Override
    public
    void removeFileAccess () {
        this.dataProvider.removeFileReferences();
    }

    /**
     * Save account dataManagement.
     *
     * @param toAdd the to add
     *
     * @return true, if successful
     */
    public
    boolean saveAccountData (final AuthenticationData toAdd) {
        var accountRegistered = false;
        var alreadyRegistered = false;
        Set<AuthenticationData> regClients;

        if (toAdd != null) {
            final var lockAlreadyAcquired = this.dataProvider.checkForActiveLock();

            if (!lockAlreadyAcquired) {
                if(!this.dataProvider.acquireAccess(true))
                    return false;
            }
            regClients = readRegisteredClients();

            for (final AuthenticationData authData : regClients) {

                if (authData.sameLogin(toAdd)) {
                    alreadyRegistered = true;
                    break;
                }
            }

            if (!alreadyRegistered && regClients.add(toAdd) &&
                this.dataProvider.writeData(regClients)) {
                accountRegistered = true;
                LOGGER.info("Account erstellt.");
            }

            if (!lockAlreadyAcquired) {
                this.dataProvider.releaseAccess();
            }
        }

        return accountRegistered;
    }
}
