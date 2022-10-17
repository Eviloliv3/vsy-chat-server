package de.vsy.server.server.server_connection;

import java.io.IOException;
import java.net.ServerSocket;

public class RemoteFollowerServerConnectionData implements ServerConnectionDataProvider {

	private final int remoteServerId;
	private final ServerSocket masterSocket;

	private RemoteFollowerServerConnectionData(final int remoteServerId, final ServerSocket masterSocket) {
		this.remoteServerId = remoteServerId;
		this.masterSocket = masterSocket;
	}

	public RemoteFollowerServerConnectionData valueOf(final int remoteServerId, final ServerSocket masterSocket) {
		return new RemoteFollowerServerConnectionData(remoteServerId, masterSocket);
	}

	public ServerSocket getConnectionSocket() {
		return this.masterSocket;
	}

	@Override
	public String getHostname() {
		return this.masterSocket.getInetAddress().getHostName();
	}

	@Override
	public int getServerPort() {
		return this.masterSocket.getLocalPort();
	}

	@Override
	public int getServerId() {
		return this.remoteServerId;
	}

	@Override
	public boolean closeConnection() {
		try {
			this.masterSocket.close();
		} catch (IOException e) {
			Thread.currentThread().interrupt();
		}
		return this.masterSocket.isClosed();
	}
}
