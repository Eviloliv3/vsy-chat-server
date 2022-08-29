package de.vsy.chat.server.persistent_data.client_data;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("pendingPacketEnum")
public
enum PendingType {
    CLIENT_BOUND,
    PROCESSOR_BOUND
}
