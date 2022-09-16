package de.vsy.server.persistent_data.server_data.temporal;

import com.fasterxml.jackson.databind.JavaType;
import de.vsy.server.persistent_data.PersistenceDAO;
import de.vsy.server.persistent_data.PersistentDataFileCreator.DataFileDescriptor;
import de.vsy.server.persistent_data.server_data.ServerDataAccess;
import de.vsy.server.server.client_management.ClientState;
import de.vsy.server.server.client_management.CurrentClientState;
import de.vsy.shared_transmission.shared_transmission.packet.property.packet_category.PacketCategory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance;
import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_SERVER_ID;

/**
 * Bietet Lese-/Schreibzugriff auf redundant gesicherte Klientenzustände in
 * JSON-Dateien.
 */
public
class LiveClientStateDAO implements ServerDataAccess {

    private static final Logger LOGGER = LogManager.getLogger();
    private final PersistenceDAO dataProvider;

    /**
     * Instantiiert einen neuen Zugriffsanbieter für persistent gespeicherte
     * Klientenzustände.
     */
    public
    LiveClientStateDAO () {

        this.dataProvider = new PersistenceDAO(DataFileDescriptor.ACTIVE_CLIENTS,
                                               getDataFormat());
    }

    /**
     * Gets the dataManagement format.
     *
     * @return the java type
     */
    public static
    JavaType getDataFormat () {
        return defaultInstance().constructMapType(HashMap.class, Integer.class,
                                                  CurrentClientState.class);
    }

    public
    Map<PacketCategory, Set<Integer>> getAllExtraSubscriptions (final int clientId) {
        final Map<PacketCategory, Set<Integer>> extraSubscriptions = new EnumMap<>(
                PacketCategory.class);
        final CurrentClientState currentState;

            if(!this.dataProvider.acquireAccess(false))
                return extraSubscriptions;
        currentState = getClientState(clientId);

        if (currentState != null) {
            extraSubscriptions.putAll(currentState.getExtraSubscriptions());
        }
        this.dataProvider.releaseAccess();
        return extraSubscriptions;
    }

    /**
     * Gets the client state.
     *
     * @param clientId the client id
     *
     * @return the client state
     */
    public
    CurrentClientState getClientState (final int clientId) {
       this.dataProvider.releaseAccess();
        CurrentClientState currentClientState;
        Map<Integer, CurrentClientState> clientStateMap;

            if(!this.dataProvider.acquireAccess(false))
                return new CurrentClientState(STANDARD_SERVER_ID);
        clientStateMap = getAllActiveClientStates();
        currentClientState = clientStateMap.get(clientId);

        if (currentClientState == null) {
            currentClientState = new CurrentClientState(STANDARD_SERVER_ID);
        }
        this.dataProvider.releaseAccess();
        return currentClientState;
    }

    /**
     * Gets the all active client states.
     *
     * @return the ${e.g(1).rsfl()}
     */
    @SuppressWarnings("unchecked")
    public
    Map<Integer, CurrentClientState> getAllActiveClientStates () {
       this.dataProvider.releaseAccess();
        Map<Integer, CurrentClientState> readMap = new HashMap<>();
        Object fromFile;

        if(!this.dataProvider.acquireAccess(false))
            return readMap;
        fromFile = this.dataProvider.readData();
        this.dataProvider.releaseAccess();

        if (fromFile instanceof HashMap) {

            try {
                readMap = (Map<Integer, CurrentClientState>) fromFile;
            } catch (final ClassCastException cc) {
                LOGGER.info(
                        "ClassCastException beim Lesen der aktiven Klientenstatusdaten.");
            }
        }
        return readMap;
    }

    /**
     * Change client state.
     *
     * @param serverPort the server port
     * @param clientId the client id
     * @param newState the new state
     *
     * @return true, if successful
     */
    public
    boolean changeClientState (final int serverPort, final int clientId,
                               final ClientState newState) {
        var stateChanged = false;
        CurrentClientState clientState;
        Map<Integer, CurrentClientState> clientStateMap;

            if(this.dataProvider.acquireAccess(true))
                return false;
        clientStateMap = getAllActiveClientStates();
        clientState = clientStateMap.get(clientId);

        if (clientState == null) {
            clientState = new CurrentClientState(serverPort);
        }
        clientState.setCurrentState(newState);
        clientState.changeServerPort(serverPort);
        clientStateMap.put(clientId, clientState);
        stateChanged = this.dataProvider.writeData(clientStateMap);

        this.dataProvider.releaseAccess();

        if (stateChanged) {
            LOGGER.info("{}: {}", clientId, newState);
        }
        return stateChanged;
    }

    public
    boolean addExtraSubscription (final int clientId, PacketCategory topic,
                                  final int threadId) {
        CurrentClientState clientState;
        Map<Integer, CurrentClientState> clientStateMap;
        var subscriptionAdded = false;
            if(this.dataProvider.acquireAccess(true))
                return false;
        clientStateMap = getAllActiveClientStates();
        clientState = clientStateMap.get(clientId);

        if (clientState != null) {
            clientState.updateExtraSubscription(topic, threadId, true);
            clientStateMap.put(clientId, clientState);
            subscriptionAdded = this.dataProvider.writeData(clientStateMap);
        } else {
            LOGGER.info(
                    "{} - Zustand wird nicht verwaltet: Zusatzabonnement nicht hinzugefügt.",
                    clientId);
        }

        this.dataProvider.releaseAccess();

        return subscriptionAdded;
    }

    public
    boolean removeExtraSubscription (final int clientId, PacketCategory topic,
                                     final int threadId) {
        CurrentClientState clientState;
        Map<Integer, CurrentClientState> clientStateMap;
        var subscriptionAdded = false;
            if(this.dataProvider.acquireAccess(true))
                return false;
        clientStateMap = getAllActiveClientStates();
        clientState = clientStateMap.get(clientId);

        if (clientState != null) {
            clientState.updateExtraSubscription(topic, threadId, false);
            clientStateMap.put(clientId, clientState);
            subscriptionAdded = this.dataProvider.writeData(clientStateMap);
        } else {
            LOGGER.info(
                    "{} - Zustand wird nicht verwaltet: Zusatzabonnement nicht entfernt.",
                    clientId);
        }

        this.dataProvider.releaseAccess();

        return subscriptionAdded;
    }

    /**
     * Change client state.
     *
     * @param clientId the client id
     * @param pendingState the pending state
     *
     * @return true, if successful
     */
    public
    boolean changeClientPendingState (final int clientId,
                                      final boolean pendingState) {
        CurrentClientState clientState;
        Map<Integer, CurrentClientState> clientStateMap;
        var pendingStateChanged = false;
            if(this.dataProvider.acquireAccess(true))
                return false;
        clientStateMap = getAllActiveClientStates();
        clientState = clientStateMap.get(clientId);

        if (clientState != null) {
            clientState.setPendingFlag(pendingState);
            clientStateMap.put(clientId, clientState);
            pendingStateChanged = this.dataProvider.writeData(clientStateMap);
        } else {
            LOGGER.info(
                    "{} - Zustand wird nicht verwaltet: PendingState wurde nicht geändert.",
                    clientId);
        }

        this.dataProvider.releaseAccess();

        return pendingStateChanged;
    }

    /**
     * Change reconnection state.
     *
     * @param clientId the client id
     * @param newState the newState
     *
     * @return true, if successful
     */
    public
    boolean changeReconnectionState (final int clientId, boolean newState) {
        CurrentClientState clientState;
        Map<Integer, CurrentClientState> clientStateMap;
        var reconnectionAllowed = false;
            if(this.dataProvider.acquireAccess(true))
                return false;
        clientStateMap = getAllActiveClientStates();
        clientState = clientStateMap.get(clientId);

        if (clientState != null) {
            if (clientState.setReconnectionState(newState)) {
                clientStateMap.put(clientId, clientState);
                reconnectionAllowed = this.dataProvider.writeData(clientStateMap);
            }
        }
        this.dataProvider.releaseAccess();
        return reconnectionAllowed;
    }

    @Override
    public
    void createFileAccess ()
    throws InterruptedException {
        this.dataProvider.createFileReferences();
    }

    /**
     * Gets the client pending state.
     *
     * @param clientId the client id
     *
     * @return the client pending state
     */
    public
    boolean getClientPendingState (final int clientId) {
       this.dataProvider.releaseAccess();
        var clientPending = false;
        CurrentClientState clientState;
        Map<Integer, CurrentClientState> clientStateMap;

            if(!this.dataProvider.acquireAccess(false))
                return false;
        clientStateMap = getAllActiveClientStates();
        clientState = clientStateMap.get(clientId);

        if (clientState != null) {
            clientPending = clientState.getPendingFlag();
        }
        this.dataProvider.releaseAccess();
        return clientPending;
    }

    /**
     * Gets the client pending state.
     *
     * @param clientId the client id
     *
     * @return the client pending state
     */
    public
    boolean getClientReconnectionState (final int clientId) {
       this.dataProvider.releaseAccess();
        var clientPending = false;
        CurrentClientState clientState;
        Map<Integer, CurrentClientState> clientStateMap;

            if(!this.dataProvider.acquireAccess(false))
                return false;
        clientStateMap = getAllActiveClientStates();
        clientState = clientStateMap.get(clientId);

        if (clientState != null) {
            clientPending = clientState.getReconnectionState();
        }
        this.dataProvider.releaseAccess();
        return clientPending;
    }

    /**
     * Gibt nur die Zustände lokal verbundener Klienten eines bestimmtes Servers
     * aus.
     *
     * @param serverPort the server port
     *
     * @return the local client states
     */
    public
    Map<Integer, CurrentClientState> getClientStatesForServer (
            final int serverPort) {
        Map<Integer, CurrentClientState> allClientStates;
        Map<Integer, CurrentClientState> remoteClientStates = new HashMap<>();

            if(!this.dataProvider.acquireAccess(false))
                return remoteClientStates;
        allClientStates = getAllActiveClientStates();

        for (final Map.Entry<Integer, CurrentClientState> client : allClientStates.entrySet()) {
            final var clientState = client.getValue();

            if (clientState.getServerId() == serverPort) {
                remoteClientStates.put(client.getKey(), clientState);
            }
        }

        this.dataProvider.releaseAccess();
        return remoteClientStates;
    }

    /**
     * Removes the all client states.
     *
     * @return true, if successful
     */
    public
    boolean removeAllClientStates () {
        var clientStatesRemoved = false;
        Map<Integer, CurrentClientState> clientStateMap;

            if(this.dataProvider.acquireAccess(true))
                return false;
        clientStateMap = new HashMap<>();
        clientStatesRemoved = this.dataProvider.writeData(clientStateMap);
        this.dataProvider.releaseAccess();

        if (clientStatesRemoved) {
            LOGGER.info("Klientenzustände wurden entfernt");
        }
        return clientStatesRemoved;
    }

    /**
     * Removes the client state.
     *
     * @param clientId the client id
     *
     * @return true, if successful
     */
    public
    boolean removeClientState (final int clientId) {
        Map<Integer, CurrentClientState> clientStateMap;
        var clientStateRemoved = false;

            if(this.dataProvider.acquireAccess(true))
                return false;
        clientStateMap = getAllActiveClientStates();
        clientStateRemoved = (clientStateMap.remove(clientId) != null) &&
                             this.dataProvider.writeData(clientStateMap);
        this.dataProvider.releaseAccess();

        if (clientStateRemoved) {
            LOGGER.info("Klient entfernt: {} ", clientId);
        }
        return clientStateRemoved;
    }

    @Override
    public
    void removeFileAccess () {
        this.dataProvider.removeFileReferences();
    }
}
