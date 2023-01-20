
package de.vsy.server.persistent_data.server_data;

import com.fasterxml.jackson.databind.JavaType;
import de.vsy.server.persistent_data.DataFileDescriptor;
import de.vsy.server.persistent_data.data_bean.AuthenticationData;

import java.util.HashSet;
import java.util.Set;

import static com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance;
import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_CLIENT_ID;

/**
 * Grants writing accessLimiter to a file containing all registered clients' account
 * dataManagement.
 */
public class ClientAuthPersistenceDAO extends ServerDAO {

    /**
     * Instantiates a new account modifier.
     */
    public ClientAuthPersistenceDAO() {
        super(DataFileDescriptor.REGISTERED_CLIENTS, getDataFormat());
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

        if (!super.dataProvider.acquireAccess(true)) {
            LOGGER.error("No shared read access.");
            return false;
        }
        regClients = readRegisteredClients();
        super.dataProvider.releaseAccess(true);

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

        if (!super.dataProvider.acquireAccess(true)) {
            LOGGER.error("No shared read access.");
            return new HashSet<>();
        }
        fromFile = super.dataProvider.readData();
        super.dataProvider.releaseAccess(true);

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

        if (!super.dataProvider.acquireAccess(true)) {
            LOGGER.error("No shared read access.");
            return clientId;
        }
        readList = readRegisteredClients();
        super.dataProvider.releaseAccess(true);

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

        if (!super.dataProvider.acquireAccess(false)) {
            LOGGER.error("No exclusive write access.");
            return false;
        }
        regClients = readRegisteredClients();
        accountRemoved = regClients.removeIf(credentials -> credentials.getClientId() == clientId) && super.dataProvider.writeData(regClients);
        super.dataProvider.releaseAccess(false);

        return accountRemoved;
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
            if (!super.dataProvider.acquireAccess(false)) {
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

            if (!alreadyRegistered && regClients.add(toAdd) && super.dataProvider.writeData(regClients)) {
                accountRegistered = true;
                LOGGER.info("Account created.");
            }

            super.dataProvider.releaseAccess(false);
        }

        return accountRegistered;
    }
}
