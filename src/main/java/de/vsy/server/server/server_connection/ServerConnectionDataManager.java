package de.vsy.server.server.server_connection;

import static java.util.Set.copyOf;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class ServerConnectionDataManager {

	private final LocalServerConnectionData clientReceptionConnectionData;
	private final Queue<RemoteServerConnectionData> notSynchronizedRemoteServers;
	private final Set<RemoteServerConnectionData> synchronizedRemoteServers;
	private boolean contactsArePresent;
	private LocalServerConnectionData serverReceptionConnectionData;

	public ServerConnectionDataManager(LocalServerConnectionData clientReceptionConnectionData) {
		this.contactsArePresent = false;
		this.clientReceptionConnectionData = clientReceptionConnectionData;
		this.notSynchronizedRemoteServers = new LinkedList<>();
		this.synchronizedRemoteServers = new HashSet<>(1);
	}

	public boolean addServerReceptionConnectionData(final LocalServerConnectionData serverConnectionData) {
		var connectionDataSet = this.serverReceptionConnectionData == null;

		if (connectionDataSet) {
			this.serverReceptionConnectionData = serverConnectionData;
		}
		return connectionDataSet;
	}

	public LocalServerConnectionData getServerReceptionConnectionData() {
		return this.serverReceptionConnectionData;
	}

	public Set<RemoteServerConnectionData> getAllSynchronizedRemoteServers() {
		return copyOf(this.synchronizedRemoteServers);
	}

	public RemoteServerConnectionData getDistinctNodeData(final Set<Integer> serverIdSet) {
		RemoteServerConnectionData foundServerNode = null;

		for (var currentNodeData : this.synchronizedRemoteServers) {

			if (!serverIdSet.contains(currentNodeData.getServerId())) {
				foundServerNode = currentNodeData;
				break;
			}
		}
		return foundServerNode;
	}

	public RemoteServerConnectionData getNextNotSynchronizedConnectionData() {
		return this.notSynchronizedRemoteServers.remove();
	}

	public LocalServerConnectionData getLocalServerConnectionData() {
		return this.clientReceptionConnectionData;
	}

	public boolean addSynchronizedConnectionData(RemoteServerConnectionData remoteConnectionData) {
		var serverConnectionAdded = false;

		if (remoteConnectionData != null) {
			removeRemoteConnectionData(remoteConnectionData);
			serverConnectionAdded = this.synchronizedRemoteServers.add(remoteConnectionData);
		}
		return serverConnectionAdded;
	}

	public void removeRemoteConnectionData(final RemoteServerConnectionData remoteConnectionData) {

		if (remoteConnectionData != null) {
			this.notSynchronizedRemoteServers.remove(remoteConnectionData);
			this.synchronizedRemoteServers.remove(remoteConnectionData);
		}
	}

	public boolean addNotSynchronizedConnectionData(RemoteServerConnectionData serverConnectionData) {
		var connectionAdded = false;

		if (serverConnectionData != null && !this.notSynchronizedRemoteServers.contains(serverConnectionData)) {
			connectionAdded = this.notSynchronizedRemoteServers.add(serverConnectionData);
		}
		return connectionAdded;
	}

	/**
	 * Schließt alle Verbindungen. Zunächst Klientenrezeption und Serverrezeption ->
	 * es kommen keine neuen Verbindungen dazu
	 */
	public void closeAllConnections() {
		this.closeConnection(clientReceptionConnectionData);
		this.closeConnection(this.serverReceptionConnectionData);

		this.closeConnections(this.notSynchronizedRemoteServers);
		this.closeConnections(this.synchronizedRemoteServers);
	}

	private void closeConnection(ServerConnectionDataProvider connection) {
		try {
			connection.closeConnection();
		} catch (IOException ioe) {
			/* Die Verbindung wird "leise" geschlossen */
		}
	}

	private void closeConnections(Collection<RemoteServerConnectionData> toClose) {

		for (final var currentConnection : toClose) {
			closeConnection(currentConnection);
		}
	}

	public void endPendingState() {
		this.contactsArePresent = true;
	}

	public boolean remoteConnectionsLive() {
		return this.notSynchronizedRemoteServers.isEmpty();
	}

	public boolean pendingConnectionStatus() {
		return !this.contactsArePresent;
	}

	public boolean noLiveServers() {
		return this.notSynchronizedRemoteServers.isEmpty() && this.synchronizedRemoteServers.isEmpty();
	}
}
