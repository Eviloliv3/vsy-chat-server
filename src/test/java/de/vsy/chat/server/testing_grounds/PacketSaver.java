package de.vsy.chat.server.testing_grounds;

import com.fasterxml.jackson.databind.JavaType;
import de.vsy.server.persistent_data.DataFileDescriptor;
import de.vsy.server.persistent_data.client_data.ClientDAO;
import de.vsy.shared_transmission.packet.Packet;

import static com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance;

public class PacketSaver extends ClientDAO {

    public PacketSaver() {
        super(DataFileDescriptor.PENDING_PACKETS, getType());
    }

    public static JavaType getType() {
        return defaultInstance().constructType(Packet.class);
    }

    public void savePacket(Packet toWrite) {
        try {
            super.dataProvider.acquireAccess(false);
            super.dataProvider.writeData(toWrite);
        } finally {
            super.dataProvider.releaseAccess(false);
        }
    }

    public Packet readPacket() {
        try {
            super.dataProvider.acquireAccess(true);
            return (Packet) super.dataProvider.readData();
        } finally {
            super.dataProvider.releaseAccess(true);
        }
    }
}
