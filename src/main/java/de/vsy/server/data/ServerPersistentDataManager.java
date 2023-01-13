package de.vsy.server.data;

import de.vsy.server.persistent_data.server_data.*;
import de.vsy.server.persistent_data.server_data.temporal.LiveClientStateDAO;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages read and write objects referencing global server data.
 */
public class ServerPersistentDataManager {

    /**
     * Grants access to persistent global client states.
     */
    public final LiveClientStateDAO clientStateAccessManager;
    /**
     * Grants access to persistent global client authentication data.
     */
    public final ClientAuthPersistenceDAO authenticationAccessManager;
    /**
     * Grants access to persistent global client representation data.
     */
    public final CommunicatorPersistenceDAO communicationEntityAccessManager;
    public final IdProvider idProvider;
    public final ClientTransactionDAO transactionAccessManager;
    private final List<SimplePathAccess> accessController;

    /**
     * Instantiates a new server persistent dataManagement manager.
     */
    public ServerPersistentDataManager(final SocketConnectionDataManager serverConnections) {
        this.accessController = new ArrayList<>();

        this.idProvider = new IdProvider();
        this.clientStateAccessManager = new LiveClientStateDAO(serverConnections);
        this.transactionAccessManager = new ClientTransactionDAO();
        this.authenticationAccessManager = new ClientAuthPersistenceDAO();
        this.communicationEntityAccessManager = new CommunicatorPersistenceDAO();

        setupAccessControl();
    }

    /**
     * Setup accessLimiter control.
     */
    private void setupAccessControl() {
        this.accessController.add(this.idProvider);
        this.accessController.add(this.clientStateAccessManager);
        this.accessController.add(this.authenticationAccessManager);
        this.accessController.add(this.transactionAccessManager);
        this.accessController.add(this.communicationEntityAccessManager);
    }

    /**
     * Initiate persistent accessLimiter.
     *
     * @throws IllegalStateException    the illegal state exception
     * @throws IllegalArgumentException the illegal argument exception
     */
    public void initiatePersistentAccess() throws IllegalStateException, IllegalArgumentException {

        for (final var currentProvider : this.accessController) {
            currentProvider.createFileAccess();
        }
    }

    /**
     * Removes the persistent accessLimiter.
     */
    public void removePersistentAccess() {

        for (final SimplePathAccess currentProvider : this.accessController) {
            if (currentProvider != null) {
                currentProvider.removeFileAccess();
            }
        }
    }

    public ClientTransactionDAO getTransactionAccessManager() {
        return this.transactionAccessManager;
    }

    public CommunicatorPersistenceDAO getCommunicationEntityAccessManager() {
        return this.communicationEntityAccessManager;
    }

    public LiveClientStateDAO getClientStateAccessManager() {
        return this.clientStateAccessManager;
    }

    public ClientAuthPersistenceDAO getClientAuthenticationAccessManager() {
        return this.authenticationAccessManager;
    }

    public IdProvider getIdProvider() {
        return this.idProvider;
    }
}
