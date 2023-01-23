
package de.vsy.chat.server.server_test_helpers;

import de.vsy.shared_module.packet_management.OutputBuffer;
import de.vsy.shared_module.packet_management.PacketBuffer;
import de.vsy.shared_transmission.dto.CommunicatorDTO;
import de.vsy.shared_transmission.packet.Packet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

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
        outputHandle.appendPacket(output);
        CountDownLatch latch = new CountDownLatch(1);

        new Timer().scheduleAtFixedRate(new TimerTask(){
            @Override
            public void run() {
                if(!(outputHandle.containsPackets())){
                    latch.countDown();
                }
            }
        }, 5, 5);
        try {
            latch.await();
        } catch (InterruptedException ie){
            Assertions.fail("Interrupted while sending " + output);
        }
    }

}
