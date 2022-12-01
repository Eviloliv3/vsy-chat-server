package de.vsy.server.persistent_data.client_data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("pendingPacketEnum")
public enum PendingType {
  @JsonProperty("CLIENT_BOUND")
  CLIENT_BOUND,
  @JsonProperty("PROCESSOR_BOUND")
  PROCESSOR_BOUND
}
