package de.vsy.server.client_handling.data_management;

import de.vsy.server.client_handling.strategy.VolatilePacketIdentifier;
import de.vsy.server.persistent_data.client_data.PendingPacketDAO;
import de.vsy.server.persistent_data.client_data.PendingType;
import de.vsy.shared_transmission.packet.Packet;

import java.util.LinkedHashMap;
import java.util.Map;

import static de.vsy.server.persistent_data.client_data.PendingType.PROCESSOR_BOUND;

public class PendingPacketCleaner {
    private PendingPacketCleaner(){}

    public static void removeVolatilePackets(PendingPacketDAO pendingPacketProvider) {
        if(pendingPacketProvider == null){
            throw new IllegalArgumentException("No PendingPacketDAO.");
        }

        Map<String, Packet> remainingPackets = new LinkedHashMap<>();
        Map<String, Packet> pendingPackets = pendingPacketProvider.readPendingPackets(PROCESSOR_BOUND);

        if(!(pendingPackets.isEmpty())) {

            for (final var currentPacketEntry : pendingPackets.entrySet()) {
                final var currentPacket = currentPacketEntry.getValue();

                if (!(VolatilePacketIdentifier.checkPacketVolatility(currentPacket))) {
                    remainingPackets.put(currentPacketEntry.getKey(), currentPacket);
                }
            }
            pendingPacketProvider.setPendingPackets(PROCESSOR_BOUND, remainingPackets);
        }
    }
}
