package de.vsy.chat.server.testing_grounds;

import com.fasterxml.jackson.databind.JavaType;
import de.vsy.server.persistent_data.PersistenceDAO;
import de.vsy.server.persistent_data.PersistentDataFileCreator.DataFileDescriptor;
import de.vsy.server.persistent_data.client_data.ClientDataAccess;
import de.vsy.shared_transmission.packet.Packet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance;
import static java.lang.String.valueOf;

public class PacketSaver implements ClientDataAccess {

    private static final Logger LOGGER = LogManager.getLogger();
    private final PersistenceDAO persistance;

    public PacketSaver() {

        persistance = new PersistenceDAO(DataFileDescriptor.PENDING_PACKETS, getType());
    }

    public static JavaType getType() {
        return defaultInstance().constructType(Packet.class);
    }

    public void savePacket(Packet toWrite) {
        try {
            persistance.acquireAccess(true);
            persistance.writeData(toWrite);
        } finally {
            persistance.releaseAccess(true);
        }
    }

    public Packet readPacket() {
        try {
            persistance.acquireAccess(false);
            return (Packet) persistance.readData();
        } finally {
            persistance.releaseAccess(false);
        }
    }

    @Override
    public void removeFileAccess() {
        persistance.removeFileReferences();
    }

    @Override
    public void createFileAccess(int clientId) throws InterruptedException {
        persistance.createFileReferences(valueOf(clientId));
    }
}
