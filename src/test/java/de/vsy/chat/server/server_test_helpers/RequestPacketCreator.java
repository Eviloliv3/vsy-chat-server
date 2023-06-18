package de.vsy.chat.server.server_test_helpers;

import de.vsy.shared_module.packet_management.PacketBuffer;
import de.vsy.shared_transmission.dto.CommunicatorDTO;
import de.vsy.shared_transmission.packet.Packet;
import org.junit.jupiter.api.Assertions;

import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * Creation tool for simple Packetcreation and transmission.
 */
public class RequestPacketCreator {

    private final Map<PacketPropertiesProvider, List<Class<?>>> associations;
    private final PacketBuffer outputHandle;

    /**
     * Instantiates a new request creator.
     *
     * @param outputHandle the output handle
     * @param clientData   the client dataManagement
     */
    public RequestPacketCreator(final PacketBuffer outputHandle, final CommunicatorDTO clientData) {
        this.outputHandle = outputHandle;
        this.associations = new HashMap<>();
        // setupPropertyAssociations(clientData);
    }

    /**
     * Send request.
     *
     * @param output the output
     */
    public void sendRequest(final Packet output) {
        CountDownLatch latch = new CountDownLatch(1);
        var timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!(outputHandle.containsPackets())) {
                    latch.countDown();
                }
            }
        }, 5, 5);
        outputHandle.appendPacket(output);

        try {
            latch.await();
        } catch (InterruptedException ie) {
            Assertions.fail("Interrupted while sending " + output);
        } finally {
            timer.cancel();
            timer.purge();
        }
    }

}
