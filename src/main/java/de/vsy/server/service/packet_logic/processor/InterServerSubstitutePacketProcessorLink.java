package de.vsy.server.service.packet_logic.processor;

import java.util.Map;

import de.vsy.server.client_handling.packet_processing.processor.AbstractPacketProcessorLink;
import de.vsy.server.persistent_data.client_data.PendingPacketDAO;
import de.vsy.server.persistent_data.client_data.PendingType;
import de.vsy.shared_module.shared_module.packet_management.PacketBuffer;
import de.vsy.shared_transmission.shared_transmission.packet.Packet;

public class InterServerSubstitutePacketProcessorLink extends AbstractPacketProcessorLink {

	private final Map<Integer, PendingPacketDAO> clientPersistenceAccessManagers;
	private final PacketBuffer requestAssignmentBuffer;

	public InterServerSubstitutePacketProcessorLink(Map<Integer, PendingPacketDAO> clientPersistenceAccessManagers,
			final PacketBuffer assignmentBuffer) {
		super(null);
		this.clientPersistenceAccessManagers = clientPersistenceAccessManagers;
		this.requestAssignmentBuffer = assignmentBuffer;
	}

	@Override
	public void processPacket(Packet input) {
		final var recipientId = input.getPacketProperties().getRecipient().getEntityId();
		final var clientPersistence = this.clientPersistenceAccessManagers.get(recipientId);

		if (clientPersistence != null) {
			clientPersistence.persistPacket(PendingType.PROCESSOR_BOUND, input);
		} else {
			this.requestAssignmentBuffer.appendPacket(input);
		}
	}
}
