package de.vsy.server.server.data;

import java.util.ArrayList;
import java.util.List;

import de.vsy.server.persistent_data.server_data.ClientAuthPersistenceDAO;
import de.vsy.server.persistent_data.server_data.ClientTransactionDAO;
import de.vsy.server.persistent_data.server_data.CommunicatorPersistenceDAO;
import de.vsy.server.persistent_data.server_data.IdProvider;
import de.vsy.server.persistent_data.server_data.ServerDataAccess;
import de.vsy.server.persistent_data.server_data.temporal.LiveClientStateDAO;

/** Verwaltet Lese-/Schreibeinheiten für Zugriff auf servereigene Daten. */
public class ServerPersistentDataManager {

	/** Speichert während der Serverlaufzeit Klientenzustände. */
	public final LiveClientStateDAO clientStateAccessManager;
	/** Gewährt Zugriff auf Daten zur Authentifizierung von Klienten. */
	public final ClientAuthPersistenceDAO authenticationAccessManager;
	/** Gewährt Zugriff auf chatbezogene (Identiäts-)Daten. */
	public final CommunicatorPersistenceDAO communicationEntityAccessManager;
	public final IdProvider idProvider;
	public final ClientTransactionDAO transactionAccessManager;
	private final List<ServerDataAccess> accessController;

	/** Instantiates a new server persistant dataManagement manager. */
	public ServerPersistentDataManager() {
		this.accessController = new ArrayList<>();

		this.idProvider = new IdProvider();
		this.clientStateAccessManager = new LiveClientStateDAO();
		this.transactionAccessManager = new ClientTransactionDAO();
		this.authenticationAccessManager = new ClientAuthPersistenceDAO();
		this.communicationEntityAccessManager = new CommunicatorPersistenceDAO();

		setupAccessControl();
	}

	/** Setup accessLimiter control. */
	private void setupAccessControl() {
		this.accessController.add(this.idProvider);
		this.accessController.add(this.clientStateAccessManager);
		this.accessController.add(this.authenticationAccessManager);
		this.accessController.add(this.transactionAccessManager);
		this.accessController.add(this.communicationEntityAccessManager);
	}

	/**
	 * Initiate persistant accessLimiter.
	 *
	 * @throws IllegalStateException    the illegal state exception
	 * @throws IllegalArgumentException the illegal argument exception
	 */
	public void initiatePersistentAccess() throws IllegalStateException, IllegalArgumentException {

		for (final var currentProvider : this.accessController) {
			try {
				currentProvider.createFileAccess();
			} catch (InterruptedException ie) {
				removePersistentAccess();
				throw new IllegalStateException(ie);
			}
		}
	}

	/** Removes the persistant accessLimiter. */
	public void removePersistentAccess() {

		for (final ServerDataAccess currentProvider : this.accessController) {

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
