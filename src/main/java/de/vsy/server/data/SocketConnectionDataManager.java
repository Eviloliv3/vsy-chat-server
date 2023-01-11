package de.vsy.server.data;

import de.vsy.server.data.socketConnection.LocalServerConnectionData;
import de.vsy.server.data.socketConnection.RemoteServerConnectionData;
import de.vsy.server.data.socketConnection.ServerConnectionDataProvider;
import de.vsy.server.data.socketConnection.SocketConnectionState;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static de.vsy.server.data.socketConnection.SocketConnectionState.INITIATED;
import static java.util.Arrays.asList;
import static java.util.List.copyOf;

/*
 * Manages connection initiation sockets (server and client directed) and inter server connections.
 */
public class SocketConnectionDataManager implements SocketInitiationCheck {

    private final ReadWriteLock lock;
    private final Condition noUninitiated;
    private final LocalServerConnectionData clientReceptionConnectionData;
    private final Map<SocketConnectionState, Queue<RemoteServerConnectionData>> remoteServerConnections;
    private LocalServerConnectionData serverReceptionConnectionData;

    {
        this.remoteServerConnections = new EnumMap<>(SocketConnectionState.class);
        this.remoteServerConnections.put(INITIATED, new LinkedList<>());
        this.remoteServerConnections.put(SocketConnectionState.PENDING, new LinkedList<>());
        this.remoteServerConnections.put(SocketConnectionState.UNINITIATED, new LinkedList<>());
    }

    public SocketConnectionDataManager(LocalServerConnectionData clientReceptionConnectionData) {
        this.lock = new ReentrantReadWriteLock();
        this.noUninitiated = lock.writeLock().newCondition();
        this.clientReceptionConnectionData = clientReceptionConnectionData;
    }

    public boolean addServerReceptionConnectionData(
            final LocalServerConnectionData serverConnectionData) {
        var connectionDataSet = this.serverReceptionConnectionData == null;

        if (connectionDataSet) {
            this.serverReceptionConnectionData = serverConnectionData;
        }
        return connectionDataSet;
    }

    public LocalServerConnectionData getServerReceptionConnectionData() {
        return this.serverReceptionConnectionData;
    }

    public boolean addServerConnection(final SocketConnectionState state,
                                       final RemoteServerConnectionData connection) {
        this.lock.writeLock().lock();

        try {
            var connectionQueue = this.remoteServerConnections.get(state);
            Set<SocketConnectionState> otherStates = new HashSet<>(
                    asList(SocketConnectionState.values()));
            otherStates.remove(state);

            for (var currentState : otherStates) {

                if (this.remoteServerConnections.get(currentState).remove(connection)) {
                    break;
                }
            }

            if (!connectionQueue.contains(connection)) {
                return connectionQueue.add(connection);
            }
            return false;
        } finally {
            if (this.remoteServerConnections.get(SocketConnectionState.UNINITIATED).isEmpty()) {
                this.noUninitiated.signal();
            }
            this.lock.writeLock().unlock();
        }
    }

    public boolean removeServerConnection(final RemoteServerConnectionData connection) {
        var connectionRemoved = false;
        this.lock.writeLock().lock();

        try {

            for (var stateSets : this.remoteServerConnections.entrySet()) {
                connectionRemoved = stateSets.getValue().remove(connection);

                if (connectionRemoved) {
                    break;
                }
            }
        } finally {
            this.lock.writeLock().unlock();
        }
        return connectionRemoved;
    }

    public boolean uninitiatedConnectionsRemaining() {
        this.lock.readLock().lock();

        try {
            return !this.remoteServerConnections.get(SocketConnectionState.UNINITIATED).isEmpty();
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public RemoteServerConnectionData getNextSocketConnectionToInitiate() {
        this.lock.writeLock().lock();

        try {
            var nextConnection = this.remoteServerConnections.get(SocketConnectionState.UNINITIATED)
                    .peek();

            if (nextConnection != null) {
                addServerConnection(SocketConnectionState.PENDING, nextConnection);
            }
            return nextConnection;
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    public Collection<RemoteServerConnectionData> getServerConnections(SocketConnectionState state) {
        this.lock.readLock().lock();

        try {
            return copyOf(this.remoteServerConnections.get(state));
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public RemoteServerConnectionData getLiveServerConnection(final int wantedServerId) {
        this.lock.readLock().lock();

        try {
            final var initiatedConnectedServers = this.remoteServerConnections.get(INITIATED);
            for (final var remoteServer : initiatedConnectedServers) {
                if (remoteServer.getServerId() == wantedServerId) {
                    return remoteServer;
                }
            }
        } finally {
            this.lock.readLock().unlock();
        }
        return null;
    }

    public RemoteServerConnectionData getDistinctNodeData(final SocketConnectionState state,
                                                          final Set<Integer> serverIdSet) {
        this.lock.readLock().lock();

        try {
            for (var currentNodeData : this.remoteServerConnections.get(state)) {

                if (!serverIdSet.contains(currentNodeData.getServerId())) {
                    return currentNodeData;
                }
            }
        } finally {
            this.lock.readLock().unlock();
        }
        return null;
    }

    public LocalServerConnectionData getLocalServerConnectionData() {
        return this.clientReceptionConnectionData;
    }

    /**
     * Closes all connections in the following order: client connection initiation, server connection
     * initiation, existing inter server connections
     */
    public void closeAllConnections() {
        this.closeConnection(clientReceptionConnectionData);
        this.closeConnection(this.serverReceptionConnectionData);

        for (var currentState : SocketConnectionState.values()) {
            this.closeConnections(this.remoteServerConnections.get(currentState));
        }
    }

    private void closeConnection(ServerConnectionDataProvider connection) {
        try {
            connection.closeConnection();
        } catch (IOException ioe) {
            /* Connection closes silently */
        }
    }

    private void closeConnections(Collection<RemoteServerConnectionData> toClose) {

        for (final var currentConnection : toClose) {
            closeConnection(currentConnection);
        }
    }

    @Override
    public void waitForUninitiatedConnections() throws InterruptedException {
        this.lock.writeLock().lock();

        try {
            while (!this.remoteServerConnections.get(SocketConnectionState.UNINITIATED).isEmpty() &&
                    !Thread.currentThread().isInterrupted()) {
                this.noUninitiated.await();
            }
        } finally {
            this.lock.writeLock().unlock();
        }
    }
}
