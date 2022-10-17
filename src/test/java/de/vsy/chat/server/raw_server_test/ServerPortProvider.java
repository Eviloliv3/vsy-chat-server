package de.vsy.chat.server.raw_server_test;

import static java.util.List.of;

import java.util.List;

public class ServerPortProvider {

	public static final ServerPortProvider SINGLE_SERVER_PORT_PROVIDER = new ServerPortProvider(of(7370));
	public static final ServerPortProvider DUAL_SERVER_PORT_PROVIDER = new ServerPortProvider(of(7370, 7371));
	private final List<Integer> usablePorts;
	private int serverPortIndex = 0;

	public ServerPortProvider(List<Integer> usablePorts) {
		if (usablePorts.isEmpty()) {
			throw new IllegalArgumentException("Keine Ports angegeben.");
		}
		this.usablePorts = usablePorts;
	}

	public int getCurrentPortIndex() {
		return this.serverPortIndex;
	}

	public int getCurrentServerPort() {
		return this.usablePorts.get(serverPortIndex);
	}

	public int getNextServerPort() {
		if (this.serverPortIndex < this.usablePorts.size() - 1) {
			this.serverPortIndex++;
		} else {
			this.serverPortIndex = 0;
		}
		return this.usablePorts.get(serverPortIndex);
	}
}
