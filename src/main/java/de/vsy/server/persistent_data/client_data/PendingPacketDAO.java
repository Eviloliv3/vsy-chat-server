/*
 *
 */
package de.vsy.server.persistent_data.client_data;

import com.fasterxml.jackson.databind.JavaType;
import de.vsy.server.persistent_data.PersistenceDAO;
import de.vsy.server.persistent_data.PersistentDataFileCreator.DataFileDescriptor;
import de.vsy.shared_transmission.shared_transmission.packet.Packet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance;
import static java.lang.String.valueOf;

/**
 * Grants writing accessLimiter to the file containing a client's pending Packet.
 */
public
class PendingPacketDAO implements ClientDataAccess, PendingPacketPersistence {

    private static final Logger LOGGER = LogManager.getLogger();
    private final PersistenceDAO dataProvider;

    /** Instantiates a new pending Packetadder. */
    public
    PendingPacketDAO () {

        this.dataProvider = new PersistenceDAO(DataFileDescriptor.PENDING_PACKETS,
                                               getDataFormat());
    }

    /**
     * Gets the dataManagement format.
     *
     * @return the dataManagement format
     */
    public static
    JavaType getDataFormat () {
        final var factory = defaultInstance();
        final var mapKey = factory.constructType(PendingType.class);
        final var linkedPendingMap = factory.constructMapType(LinkedHashMap.class,
                                                              String.class,
                                                              Packet.class);
        return factory.constructMapType(EnumMap.class, mapKey, linkedPendingMap);
    }

    @Override
    public
    void createFileAccess (final int clientId)
    throws InterruptedException {
        this.dataProvider.createFileReferences(valueOf(clientId));
    }

    @Override
    public
    boolean persistPacket (final PendingType classification,
                           final Packet toPersist) {
        return appendPendingPacket(classification, toPersist);
    }

    /**
     * Append pending Packet
     *
     * @param classification the classification
     * @param toAppend the to append
     *
     * @return true, if successful
     */
    public
    boolean appendPendingPacket (final PendingType classification,
                                 final Packet toAppend) {
        var packetAdded = false;
        Map<String, Packet> pendingMap;

        if (toAppend != null) {
            final var lockAlreadyAcquired = this.dataProvider.checkForActiveLock();

            if (!lockAlreadyAcquired) {
                this.dataProvider.acquireAccess(true);
            }
            pendingMap = readPendingPackets(classification);

            packetAdded =
                    pendingMap.putIfAbsent(toAppend.getPacketHash(), toAppend) ==
                    null;

            if (packetAdded) {
                packetAdded = setPendingPackets(classification, pendingMap);
            }

            if (!lockAlreadyAcquired) {
                this.dataProvider.releaseAccess();
            }

            if (packetAdded) {
                LOGGER.info("PendingPacket hinzugefügt.");
            }
        }
        return packetAdded;
    }

    /**
     * Read pending Packet.
     *
     * @param classification the classification
     *
     * @return theArrayList
     */
    public
    Map<String, Packet> readPendingPackets (final PendingType classification) {
        Map<PendingType, LinkedHashMap<String, Packet>> allPendingPackets;
        Map<String, Packet> pendingMap;

        final var lockAlreadyAcquired = this.dataProvider.checkForActiveLock();

        if (!lockAlreadyAcquired) {
            this.dataProvider.acquireAccess(false);
        }
        allPendingPackets = readAllPendingPackets();
        pendingMap = allPendingPackets.get(classification);

        if (!lockAlreadyAcquired) {
            this.dataProvider.releaseAccess();
        }

        if (pendingMap == null) {
            pendingMap = new LinkedHashMap<>();
        }
        return pendingMap;
    }

    /**
     * Sets the pending Packet.
     *
     * @param classification the classification
     * @param toSet the to set
     *
     * @return true, if successful
     */
    public
    boolean setPendingPackets (final PendingType classification,
                               final Map<String, Packet> toSet) {
        var packetAdded = false;
        Map<PendingType, LinkedHashMap<String, Packet>> allPendingPackets;
        LinkedHashMap<String, Packet> classifiedPendingPackets;

        if (toSet != null) {
            final boolean lockAlreadyAcquired;
            classifiedPendingPackets = new LinkedHashMap<>(toSet);
            lockAlreadyAcquired = this.dataProvider.checkForActiveLock();

            if (!lockAlreadyAcquired) {
                this.dataProvider.acquireAccess(true);
            }
            allPendingPackets = readAllPendingPackets();
            allPendingPackets.put(classification, classifiedPendingPackets);
            packetAdded = this.dataProvider.writeData(allPendingPackets);

            if (!lockAlreadyAcquired) {
                this.dataProvider.releaseAccess();
            }
        }
        return packetAdded;
    }

    /**
     * Read all pending Packet.
     *
     * @return the map
     */
    @SuppressWarnings("unchecked")
    public
    Map<PendingType, LinkedHashMap<String, Packet>> readAllPendingPackets () {
        EnumMap<PendingType, LinkedHashMap<String, Packet>> allPendingPackets = null;
        Object fromFile;

        final var lockAlreadyAcquired = this.dataProvider.checkForActiveLock();

        if (!lockAlreadyAcquired) {
            this.dataProvider.acquireAccess(false);
        }

        fromFile = this.dataProvider.readData();

        if (!lockAlreadyAcquired) {
            this.dataProvider.releaseAccess();
        }

        if (fromFile instanceof EnumMap) {
            try {
                allPendingPackets = (EnumMap<PendingType, LinkedHashMap<String, Packet>>) fromFile;
            } catch (final ClassCastException cc) {
                LOGGER.info(
                        "ClassCastException beim Lesen der verpassten PacketMap. Die verpasste PacketMap wird leer ausgegeben.");
            }
        }

        if (allPendingPackets == null) {
            allPendingPackets = new EnumMap<>(PendingType.class);

            for (final var classification : PendingType.values()) {
                allPendingPackets.put(classification, new LinkedHashMap<>());
            }
        }

        return allPendingPackets;
    }

    @Override
    public
    void removePacket (final PendingType classification, final Packet toRemove) {
        removePendingPacket(classification, toRemove);
    }

    /**
     * Removes the pending Packet
     *
     * @param classification the classification
     * @param toRemove the to remove
     */
    public
    void removePendingPacket (final PendingType classification,
                                 final Packet toRemove) {
        var packetRemoved = false;
        Map<PendingType, LinkedHashMap<String, Packet>> allPendingPackets;
        LinkedHashMap<String, Packet> pendingMap;

        final var lockAlreadyAcquired = this.dataProvider.checkForActiveLock();

        if (!lockAlreadyAcquired) {
            this.dataProvider.acquireAccess(true);
        }
        allPendingPackets = readAllPendingPackets();
        pendingMap = allPendingPackets.get(classification);
        packetRemoved = pendingMap.remove(toRemove.getPacketHash()) != null;

        if (packetRemoved) {
            allPendingPackets.put(classification, pendingMap);
            this.dataProvider.writeData(allPendingPackets);
        }

        if (!lockAlreadyAcquired) {
            this.dataProvider.releaseAccess();
        }
    }

    @Override
    public
    void removeFileAccess () {
        this.dataProvider.removeFileReferences();
    }
}
