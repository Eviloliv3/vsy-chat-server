package de.vsy.server.server_packet.packet_creation;

import de.vsy.server.client_handling.data_management.bean.LocalClientDataProvider;
import de.vsy.server.client_handling.packet_processing.processor.ResultingPacketCreator;
import de.vsy.shared_module.shared_module.packet_creation.PacketCompiler;
import de.vsy.shared_transmission.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.shared_transmission.packet.content.PacketContent;
import de.vsy.shared_transmission.shared_transmission.packet.property.communicator.CommunicationEndpoint;
import de.vsy.shared_utility.standard_value.StandardIdProvider;

public class ClientHandlerPacketCreator extends ResultingPacketCreator {

	private final LocalClientDataProvider clientDataProvider;

	public ClientHandlerPacketCreator(LocalClientDataProvider clientDataProvider) {
		this.clientDataProvider = clientDataProvider;
	}

	@Override
	public Packet createRequest(PacketContent processedContent, CommunicationEndpoint recipient) {
		if (processedContent == null) {
			throw new IllegalArgumentException("Kein Paketinhalt uebergeben.");
		}
		if (recipient == null) {
			throw new IllegalArgumentException("Kein Empfaenger uebergeben.");
		}

		final var resultingContent = adjustWrapping(processedContent, true);
		if (this.currentRequest == null) {
			return PacketCompiler.createRequest(recipient, resultingContent);
		} else {
			return PacketCompiler.createFollowUpRequest(recipient, resultingContent, this.currentRequest);
		}
	}

	@Override
	public Packet createResponse(PacketContent processedContent) {
		final var resultingContent = adjustWrapping(processedContent, false);
		return PacketCompiler.createResponse(resultingContent, this.currentRequest);
	}

	protected PacketContent adjustWrapping(final PacketContent contentToWrap, final boolean isRequest) {
		PacketContent finalContent;

		final var clientIsSender = checkClientSender();
		final var toWrap = (!isRequest && !clientIsSender) || (isRequest && clientIsSender);

		if (toWrap) {
			finalContent = wrapContent(contentToWrap);
		} else {
			finalContent = contentToWrap;
		}

		return finalContent;
	}

	protected boolean checkClientSender() {
		final var senderId = this.currentRequest.getPacketProperties().getSender().getEntityId();
		return senderId == this.clientDataProvider.getClientId() || senderId == StandardIdProvider.STANDARD_CLIENT_ID;
	}
}
