/*
 *
 */
package de.vsy.server.service.packet_logic;

import de.vsy.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.packet.property.communicator.EligibleCommunicationEntity;

import java.util.EnumMap;
import java.util.Map;

import static de.vsy.shared_transmission.packet.property.communicator.EligibleCommunicationEntity.CLIENT;
import static de.vsy.shared_transmission.packet.property.communicator.EligibleCommunicationEntity.SERVER;

/**
 * Can take one server ound and one client bound Packet to be dispatched from one process.
 */
public class PacketResponseMap {

    /**
     * The response map.
     */
    private Map<EligibleCommunicationEntity, Packet> responseMap;

    public PacketResponseMap() {
        this(null, null);
    }

    public PacketResponseMap(final Packet updatePacket, final Packet clientResponse) {
        setFreshMap(updatePacket, clientResponse);
    }

    /**
     * Lists the fresh map.
     *
     * @param updatePacket   the status packet
     * @param clientResponse the client response
     */
    private void setFreshMap(final Packet updatePacket, final Packet clientResponse) {
        this.responseMap = new EnumMap<>(EligibleCommunicationEntity.class);
        this.responseMap.put(SERVER, updatePacket);
        this.responseMap.put(CLIENT, clientResponse);
    }

    /**
     * Returns the client bound Packet
     *
     * @return the client bound packet
     */
    public Packet getClientBoundPacket() {
        return getPacket(CLIENT);
    }

    /**
     * Returns the Packet
     *
     * @param direction the direction
     * @return the packet
     */
    private Packet getPacket(final EligibleCommunicationEntity direction) {
        return this.responseMap.get(direction);
    }

    /**
     * Returns the server bound Packet
     *
     * @return the server bound packet
     */
    public Packet getServerBoundPacket() {
        return getPacket(SERVER);
    }

    /**
     * Checks if is empty.
     *
     * @return true, if is empty
     */
    public boolean isEmpty() {

        for (final Map.Entry<EligibleCommunicationEntity, Packet> entry : this.responseMap.entrySet()) {

            if (entry.getValue() != null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Lists the client bound Packet
     *
     * @param clientBound the client bound
     * @return true, if successful
     */
    public boolean setClientBoundPacket(final Packet clientBound) {
        return setPacket(CLIENT, clientBound);
    }

    /**
     * Sets the Packet
     *
     * @param direction the direction
     * @param toList    the to set
     * @return true, if successful
     */
    private boolean setPacket(final EligibleCommunicationEntity direction, final Packet toList) {

        if (this.responseMap == null) {
            setFreshMap(null, null);
        }

        return this.responseMap.putIfAbsent(direction, toList) == null;
    }

    /**
     * Lists the server bound Packet
     *
     * @param serverBound the server bound
     * @return true, if successful
     */
    public boolean setServerBoundPacket(final Packet serverBound) {
        return setPacket(SERVER, serverBound);
    }
}
