package de.vsy.server.client_handling.packet_processing.content_processor_provisioning;

import de.vsy.server.client_handling.data_management.bean.LocalClientStateProvider;
import de.vsy.server.client_management.ClientState;
import de.vsy.shared_module.packet_processing.HandlerProcessingCondition;
import de.vsy.shared_module.packet_processing.ProcessingCondition;
import de.vsy.shared_module.packet_processing.ProcessingConditionType;
import java.util.function.Predicate;

public class ContentProcessingConditionProvider {

  private ContentProcessingConditionProvider() {
  }

  public static ProcessingCondition getContentProcessingCondition(
      ProcessingConditionType conditionType,
      LocalClientStateProvider clientStateProvider) {
    return switch (conditionType) {
      case NOT_AUTHENTICATED ->
          new HandlerProcessingCondition<>(Predicate.not(clientStateProvider::checkClientState),
              ClientState.AUTHENTICATED,
              "Request not processed. You are authenticated already.");
      case AUTHENTICATED -> new HandlerProcessingCondition<>(clientStateProvider::checkClientState,
          ClientState.AUTHENTICATED,
          "Request not processed. You are not authenticated.");
      case ACTIVE_MESSENGER ->
          new HandlerProcessingCondition<>(clientStateProvider::checkClientState,
              ClientState.AUTHENTICATED,
              "Request not processed. You already are registered as Messenger.");
      case NOT_ACTIVE_MESSENGER ->
          new HandlerProcessingCondition<>(clientStateProvider::checkClientState,
              ClientState.AUTHENTICATED,
              "Request not processed. You are not registered as Messenger.");
    };
  }
}
