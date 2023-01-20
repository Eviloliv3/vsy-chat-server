
package de.vsy.server.persistent_data.client_data;

import com.fasterxml.jackson.databind.JavaType;
import de.vsy.server.persistent_data.DataFileDescriptor;
import de.vsy.shared_transmission.packet.Packet;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance;

/**
 * Grants writing accessLimiter to the file containing a client's pending Packet.
 */
public class PendingPacketDAO extends ClientDAO {

    public PendingPacketDAO() {
        super(DataFileDescriptor.PENDING_PACKETS, getDataFormat());
    }

    /**
     * Returns the dataManagement format.
     *
     * @return the dataManagement format
     */
    public static JavaType getDataFormat() {
        final var factory = defaultInstance();
        final var mapKey = factory.constructType(PendingType.class);
        final var linkedPendingMap = factory.constructMapType(LinkedHashMap.class, String.class,
                Packet.class);
        return factory.constructMapType(EnumMap.class, mapKey, linkedPendingMap);
    }

    public boolean persistPacket(final PendingType classification, final Packet toPersist) {
        return appendPendingPacket(classification, toPersist);
    }

    /**
     * Append pending Packet
     *
     * @param classification the classification
     * @param toAppend       the to append
     * @return true, if successful
     */
    public boolean appendPendingPacket(final PendingType classification, final Packet toAppend) {
        if (classification == null || toAppend == null) {
            throw new IllegalArgumentException(
                    "Invalid argument. " + classification + "/" + toAppend);
        }
        var packetAdded = false;
        Map<String, Packet> pendingMap;

        if (!super.dataProvider.acquireAccess(false)) {
            LOGGER.error("No exclusive write access.");
            return false;
        }
        pendingMap = readPendingPackets(classification);
        packetAdded = pendingMap.putIfAbsent(toAppend.getPacketHash(), toAppend) == null;

        if (packetAdded) {
            packetAdded = setPendingPackets(classification, pendingMap);
        }

        super.dataProvider.releaseAccess(false);

        if (packetAdded) {
            LOGGER.info("PendingPacket added.");
        }
        return packetAdded;
    }

    /**
     * Read pending Packet.
     *
     * @param classification the classification
     * @return theArrayList
     */
    public Map<String, Packet> readPendingPackets(final PendingType classification) {
        Map<PendingType, LinkedHashMap<String, Packet>> allPendingPackets;
        Map<String, Packet> pendingMap;

        if (!super.dataProvider.acquireAccess(true)) {
            LOGGER.error("No shared read access.");
            return new LinkedHashMap<>();
        }
        allPendingPackets = readAllPendingPackets();
        super.dataProvider.releaseAccess(true);
        pendingMap = allPendingPackets.get(classification);

        if (pendingMap == null) {
            pendingMap = new LinkedHashMap<>();
        }
        return pendingMap;
    }

    /**
     * Sets the pending Packet.
     *
     * @param classification the classification
     * @param toSet          the to set
     * @return true, if successful
     */
    public boolean setPendingPackets(final PendingType classification,
                                     final Map<String, Packet> toSet) {
        var packetAdded = false;
        Map<PendingType, LinkedHashMap<String, Packet>> allPendingPackets;
        LinkedHashMap<String, Packet> classifiedPendingPackets;

        if (toSet != null) {
            classifiedPendingPackets = new LinkedHashMap<>(toSet);

            if (!super.dataProvider.acquireAccess(false)) {
                LOGGER.error("No exclusive write access.");
                return false;
            }
            allPendingPackets = readAllPendingPackets();
            allPendingPackets.put(classification, classifiedPendingPackets);
            packetAdded = super.dataProvider.writeData(allPendingPackets);

            super.dataProvider.releaseAccess(false);
        }
        return packetAdded;
    }

    /**
     * Read all pending Packet.
     *
     * @return the map
     */
    @SuppressWarnings("unchecked")
    public Map<PendingType, LinkedHashMap<String, Packet>> readAllPendingPackets() {
        EnumMap<PendingType, LinkedHashMap<String, Packet>> allPendingPackets = null;
        Object fromFile;

        if (!super.dataProvider.acquireAccess(true)) {
            LOGGER.error("No shared read access.");
            return new EnumMap<>(PendingType.class);
        }
        fromFile = super.dataProvider.readData();
        super.dataProvider.releaseAccess(true);

        if (fromFile instanceof EnumMap) {
            try {
                allPendingPackets = (EnumMap<PendingType, LinkedHashMap<String, Packet>>) fromFile;
            } catch (final ClassCastException cc) {
                LOGGER.info(
                        "{} occurred while reading the missed packet map. Empty map will be returned.",
                        cc.getClass().getSimpleName());
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

    public void removePacket(final PendingType classification, final Packet toRemove) {
        removePendingPacket(classification, toRemove);
    }

    /**
     * Removes the pending Packet
     *
     * @param classification the classification
     * @param toRemove       the to remove
     */
    public void removePendingPacket(final PendingType classification, final Packet toRemove) {
        var packetRemoved = false;
        Map<PendingType, LinkedHashMap<String, Packet>> allPendingPackets;
        LinkedHashMap<String, Packet> pendingMap;

        if (!super.dataProvider.acquireAccess(false)) {
            LOGGER.error("No exclusive write access.");
            return;
        }
        allPendingPackets = readAllPendingPackets();
        pendingMap = allPendingPackets.get(classification);
        packetRemoved = pendingMap.remove(toRemove.getPacketHash()) != null;

        if (packetRemoved) {
            allPendingPackets.put(classification, pendingMap);
            super.dataProvider.writeData(allPendingPackets);
        }

        super.dataProvider.releaseAccess(false);
    }
}
