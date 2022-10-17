package de.vsy.server.client_handling.packet_processing.content_processor_provisioning;

import java.util.function.Predicate;

import de.vsy.server.client_handling.data_management.bean.LocalClientStateProvider;
import de.vsy.server.server.client_management.ClientState;
import de.vsy.shared_module.shared_module.packet_processing.HandlerProcessingCondition;
import de.vsy.shared_module.shared_module.packet_processing.ProcessingCondition;
import de.vsy.shared_module.shared_module.packet_processing.ProcessingConditionType;

public class ContentProcessingConditionProvider {

	private ContentProcessingConditionProvider() {
	}

	public static ProcessingCondition getContentProcessingCondition(ProcessingConditionType conditionType,
			LocalClientStateProvider clientStateProvider) {
		return switch (conditionType) {
		case NOT_AUTHENTICATED -> new HandlerProcessingCondition<>(Predicate.not(clientStateProvider::checkClientState),
				ClientState.AUTHENTICATED, "Anfrage nicht bearbeitet. Sie sind bereits authentifiziert.");
		case AUTHENTICATED -> new HandlerProcessingCondition<>(clientStateProvider::checkClientState,
				ClientState.AUTHENTICATED, "Anfrage nicht bearbeitet. Sie sind noch nicht authentifiziert.");
		case ACTIVE_MESSENGER -> new HandlerProcessingCondition<>(clientStateProvider::checkClientState,
				ClientState.AUTHENTICATED, "Anfrage nicht bearbeitet. Sie sind als Messenger registriert.");
		case NOT_ACTIVE_MESSENGER -> new HandlerProcessingCondition<>(clientStateProvider::checkClientState,
				ClientState.AUTHENTICATED, "Anfrage nicht bearbeitet. Sie sind nicht als Messenger registriert.");
		};
	}
}
