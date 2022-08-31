package de.vsy.server.client_handling.persistent_data_access;

import de.vsy.server.client_handling.data_management.bean.ClientStateListener;
import de.vsy.server.client_handling.data_management.bean.LocalClientDataProvider;
import de.vsy.server.persistent_data.client_data.ClientDataAccess;
import de.vsy.server.persistent_data.client_data.ContactListDAO;
import de.vsy.server.persistent_data.client_data.MessageDAO;
import de.vsy.server.persistent_data.client_data.PendingPacketDAO;
import de.vsy.server.server.client_management.ClientState;

import java.util.ArrayList;
import java.util.List;

/** The Class ClientPersistentDataAccessProvider. */
public
class ClientPersistentDataAccessProvider
        implements ChatHandlingPersistentAccess, ClientStateListener, RelationHandlingPersistenceAccess, UpdateHandlingPersistentAccess {

    private final LocalClientDataProvider localClientData;
    private final ContactListDAO contactDataProvider;
    private final MessageDAO messageDataProvider;
    private final PendingPacketDAO pendingPacketAccessor;
    private final List<ClientDataAccess> persistenceDAOList;
    private boolean dataAccessible;

    /**
     * Instantiates a new client persistant dataManagement accessLimiter.
     *
     * @param localClientData the local client dataManagement
     */
    public
    ClientPersistentDataAccessProvider (
            final LocalClientDataProvider localClientData) {
        this.dataAccessible = false;
        this.persistenceDAOList = new ArrayList<>();

        this.localClientData = localClientData;

        this.messageDataProvider = new MessageDAO();
        this.contactDataProvider = new ContactListDAO();
        this.pendingPacketAccessor = new PendingPacketDAO();
        initClientDataAccessList();
    }

    /** Inits the client dataManagement accessLimiter list. */
    private
    void initClientDataAccessList () {
        persistenceDAOList.add(this.messageDataProvider);
        persistenceDAOList.add(this.contactDataProvider);
        persistenceDAOList.add(this.pendingPacketAccessor);
    }

    @Override
    public
    void evaluateNewState (ClientState changedState, boolean added) {

        if (changedState.equals(ClientState.AUTHENTICATED)) {

            if (added) {
                try {
                    initiateClientDataAccess(this.localClientData.getClientId());
                    this.dataAccessible = true;
                } catch (InterruptedException ie) {
                    this.dataAccessible = false;
                    cutClientDataAccess();
                    throw new IllegalStateException(ie);
                }
            } else {
                cutClientDataAccess();
            }
        }
    }

    /**
     * Initiate client dataManagement accessLimiter.
     *
     * @param clientId the client id
     */
    public
    void initiateClientDataAccess (final int clientId)
    throws InterruptedException {
        for (final var persistenceDAO : persistenceDAOList) {
            persistenceDAO.createFileAccess(clientId);
        }
    }

    /** Cut client dataManagement accessLimiter. */
    public
    void cutClientDataAccess () {

        for (final ClientDataAccess DAO : persistenceDAOList) {
            DAO.removeFileAccess();
        }
        this.dataAccessible = false;
    }

    @Override
    public
    ContactListDAO getContactlistDAO () {
        return this.contactDataProvider;
    }

    @Override
    public
    MessageDAO getMessageDAO () {
        return this.messageDataProvider;
    }

    /**
     * Gets the pending PacketaccessLimiter provider.
     *
     * @return the pending PacketaccessLimiter provider
     */
    public
    PendingPacketDAO getPendingPacketDAO () {
        return this.pendingPacketAccessor;
    }

    public
    boolean isDataAccessible () {
        return this.dataAccessible;
    }
}
