/*
 *
 */
package de.vsy.server.service.inter_server;

import static de.vsy.shared_utility.standard_value.ThreadContextValues.LOG_FILE_CONTEXT_KEY;
import static java.lang.Thread.interrupted;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import de.vsy.server.client_handling.packet_processing.processor.PacketSyntaxCheckLink;
import de.vsy.server.exception_processing.ServerPacketHandlingExceptionCreator;
import de.vsy.server.persistent_data.client_data.PendingPacketDAO;
import de.vsy.server.persistent_data.server_data.temporal.LiveClientStateDAO;
import de.vsy.server.server.data.access.ServerCommunicationServiceDataProvider;
import de.vsy.server.server.server_connection.RemoteServerConnectionData;
import de.vsy.server.server_packet.packet_validation.ServerPacketTypeValidationCreator;
import de.vsy.server.service.Service;
import de.vsy.server.service.ServicePacketBufferManager;
import de.vsy.server.service.packet_logic.processor.InterServerSubstitutePacketProcessorLink;
import de.vsy.shared_module.shared_module.packet_exception.PacketHandlingException;
import de.vsy.shared_module.shared_module.packet_management.PacketBuffer;
import de.vsy.shared_module.shared_module.packet_processing.PacketProcessor;
import de.vsy.shared_module.shared_module.packet_validation.SimplePacketChecker;
import de.vsy.shared_module.shared_module.thread_manipulation.ProcessingInterruptProvider;
import de.vsy.shared_transmission.shared_transmission.packet.Packet;
import de.vsy.shared_utility.logging.ThreadContextRunnable;

/**
 * Service checking for pending Packetqueues that have outlived their ttl,
 * dissolving them if necessary.
 */
public class InterServerSubstituteService extends ThreadContextRunnable implements ClientReconnectionHandler {

	private static final AtomicInteger SERVICE_COUNT = new AtomicInteger(1);
	private static final Logger LOGGER = LogManager.getLogger();
	private final int serviceId;
	private final Timer reconnectionStateWatcher;
	private final Map<Integer, PendingPacketDAO> clientPersistenceAccessManagers;
	private final LiveClientStateDAO clientStateProvider;
	private final PacketBuffer interServerBuffer;
	private final RemoteServerConnectionData remoteServerConnection;
	private final ServicePacketBufferManager serviceBuffers;
	private final RemoteClientDisconnector clientDisconnector;
	private PacketProcessor processor;
	private ProcessingInterruptProvider interrupt;
	private PacketBuffer requestBuffer;

	/**
	 * Instantiates a new server failed client watcher.
	 *
	 * @param serviceDataAccess      the service dataManagement access
	 * @param remoteServerConnection the remote server connection
	 * @param interServerBuffer      the inter server buffer
	 */
	public InterServerSubstituteService(final ServerCommunicationServiceDataProvider serviceDataAccess,
			final RemoteServerConnectionData remoteServerConnection, final PacketBuffer interServerBuffer) {

		this.serviceId = SERVICE_COUNT.incrementAndGet();
		this.reconnectionStateWatcher = new Timer("ClientReconnectionStateWatcher-" + serviceId);
		this.clientPersistenceAccessManagers = new HashMap<>();
		this.remoteServerConnection = remoteServerConnection;
		this.serviceBuffers = serviceDataAccess.getServicePacketBufferManager();
		this.clientStateProvider = serviceDataAccess.getLiveClientStateDAO();
		this.interServerBuffer = interServerBuffer;
		this.clientDisconnector = new RemoteClientDisconnector(this.interServerBuffer, this.serviceBuffers,
				serviceDataAccess.getCommunicatorDataAccessor(), this.clientStateProvider,
				serviceDataAccess.getClientSubscriptionManager());
	}

	@Override
	public void runWithContext() {
		if (finishSetup()) {
			LOGGER.info("{} gestartet.", ThreadContext.get(LOG_FILE_CONTEXT_KEY));

			while (this.interrupt.conditionNotMet()) {
				processPacket();
			}
			this.clientDisconnector.disconnectRemainingClients(this.clientPersistenceAccessManagers);
			this.reconnectionStateWatcher.cancel();
			clearAndRemoveBuffer();
		}
		LOGGER.info("{} gestoppt.", ThreadContext.get(LOG_FILE_CONTEXT_KEY));
	}

	/** Finish setup. */
	private boolean finishSetup() {
		boolean substituteSetup = false;
		Instant stopTime;
		List<Integer> pendingClientIds;
		ThreadContext.put(LOG_FILE_CONTEXT_KEY, "InterServerSubstitute-" + this.serviceId);

		pendingClientIds = getPendingClientIds();

		if (!(pendingClientIds.isEmpty())) {
			preparePendingClients(pendingClientIds);

			this.serviceBuffers.registerBuffer(Service.TYPE.SERVER_TRANSFER, serviceId, this.interServerBuffer);
			this.requestBuffer = this.serviceBuffers.getRandomBuffer(Service.TYPE.REQUEST_ROUTER);
			this.processor = new PacketSyntaxCheckLink(
					new InterServerSubstitutePacketProcessorLink(this.clientPersistenceAccessManagers,
							this.requestBuffer),
					new SimplePacketChecker(
							ServerPacketTypeValidationCreator.createRegularServerPacketContentValidator()));
			this.reconnectionStateWatcher.schedule(
					new ClientReconnectionStateWatcher(this.clientStateProvider, pendingClientIds, this), 500, 1000);
			stopTime = Instant.now().plusMillis(25000);
			this.interrupt = () -> Instant.now().isAfter(stopTime);
			substituteSetup = true;
		} else {
			LOGGER.info("Es gibt keine verbundenen Klienten für Server {}", this.remoteServerConnection.getServerId());
		}
		return substituteSetup;
	}

	/** Process Packet */
	private void processPacket() {
		Packet input = null;

		try {
			input = this.interServerBuffer.getPacket();

			if (input != null) {
				processor.processPacket(input);
			}
		} catch (final PacketHandlingException phe) {
			var errorResponse = ServerPacketHandlingExceptionCreator.getServiceExceptionProcessor()
					.processException(phe, input);
			if (errorResponse != null) {
				this.requestBuffer.appendPacket(errorResponse);
			}
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
			LOGGER.error("InterServerSubstituteService beim Holen des " + "naechsten Pakets unterbrochen.");
		}
	}

	/** Clear and remove buffer. */
	private void clearAndRemoveBuffer() {
		final var reinterrupt = interrupted();
		this.serviceBuffers.deregisterBuffer(Service.TYPE.SERVER_TRANSFER, serviceId, this.interServerBuffer);

		while (this.interServerBuffer.containsPackets()) {
			processPacket();
		}

		if (reinterrupt) {
			Thread.currentThread().interrupt();
		}
	}

	private List<Integer> getPendingClientIds() {
		final var remoteClientStateMap = this.clientStateProvider
				.getClientStatesForServer(this.remoteServerConnection.getServerId());
		return new ArrayList<>(remoteClientStateMap.keySet());
	}

	/**
	 * Je Klient wird der Pendingzustand eingetragen sowie ein Kanal zur
	 * persistenten Paketspeicherung erstellt.
	 *
	 * @param pendingClientIds the pending client ids
	 */
	private void preparePendingClients(List<Integer> pendingClientIds) {

		for (final var currentClientId : pendingClientIds) {
			this.clientStateProvider.changeClientPendingState(currentClientId, true);
			try {
				final var pendingPacketAccessor = new PendingPacketDAO();
				pendingPacketAccessor.createFileAccess(currentClientId);
				this.clientPersistenceAccessManagers.put(currentClientId, pendingPacketAccessor);
			} catch (InterruptedException ie) {
				LOGGER.error("Kein Zugriff auf schwebende Pakete für Klienten: {}. Klient wird vollständig entfernt.",
						currentClientId);
				this.clientStateProvider.removeClientState(currentClientId);
			}
		}
	}

	/**
	 * Process reconnection.
	 *
	 * @param clientId the client id
	 */
	@Override
	public void processReconnection(int clientId) {

		if (this.clientPersistenceAccessManagers.containsKey(clientId)) {
			this.clientDisconnector.unsubscribeClient(clientId);
			this.clientStateProvider.changeClientPendingState(clientId, false);
			final var reconnectedClientPersistenceAccess = this.clientPersistenceAccessManagers.remove(clientId);

			if (reconnectedClientPersistenceAccess != null) {
				reconnectedClientPersistenceAccess.removeFileAccess();
			}
		}
	}
}
