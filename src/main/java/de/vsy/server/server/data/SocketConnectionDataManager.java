package de.vsy.server.server.data;

import static de.vsy.server.server.data.socketConnection.SocketConnectionState.INITIATED;
import static de.vsy.server.server.data.socketConnection.SocketConnectionState.PENDING;
import static de.vsy.server.server.data.socketConnection.SocketConnectionState.UNINITIATED;
import static java.util.Arrays.asList;
import static java.util.List.copyOf;

import de.vsy.server.server.data.socketConnection.LocalServerConnectionData;
import de.vsy.server.server.data.socketConnection.RemoteServerConnectionData;
import de.vsy.server.server.data.socketConnection.ServerConnectionDataProvider;
import de.vsy.server.server.data.socketConnection.SocketConnectionState;
import java.io.IOException;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
    this.remoteServerConnections.put(PENDING, new LinkedList<>());
    this.remoteServerConnections.put(UNINITIATED, new LinkedList<>());
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
    try {
      this.lock.writeLock().lock();
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
      if (this.remoteServerConnections.get(UNINITIATED).isEmpty()) {
        this.noUninitiated.signal();
      }
      this.lock.writeLock().unlock();
    }
  }

  public boolean removeServerConnection(final RemoteServerConnectionData connection) {
    var connectionRemoved = false;

    try {
      this.lock.writeLock().lock();

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
    try {
      this.lock.readLock().lock();
      return !this.remoteServerConnections.get(UNINITIATED).isEmpty();
    } finally {
      this.lock.readLock().unlock();
    }
  }

  public RemoteServerConnectionData getNextSocketConnectionToInitiate() {
    try {
      this.lock.writeLock().lock();
      var nextConnection = this.remoteServerConnections.get(UNINITIATED).peek();

      if (nextConnection != null) {
        addServerConnection(PENDING, nextConnection);
      }
      return nextConnection;
    } finally {
      this.lock.writeLock().unlock();
    }
  }

  public Collection<RemoteServerConnectionData> getServerConnections(SocketConnectionState state) {
    try {
      this.lock.readLock().lock();
      return copyOf(this.remoteServerConnections.get(state));
    } finally {
      this.lock.readLock().unlock();
    }
  }

  public RemoteServerConnectionData getDistinctNodeData(final SocketConnectionState state,
      final Set<Integer> serverIdSet) {

    try {
      this.lock.readLock().lock();
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
      /* Die Verbindung wird "leise" geschlossen */
    }
  }

  private void closeConnections(Collection<RemoteServerConnectionData> toClose) {

    for (final var currentConnection : toClose) {
      closeConnection(currentConnection);
    }
  }

  @Override
  public void waitForUninitiatedConnections() throws InterruptedException {
    try {
      this.lock.writeLock().lock();
      while (!this.remoteServerConnections.get(UNINITIATED).isEmpty() &&
          !Thread.currentThread().isInterrupted()) {
        this.noUninitiated.await();
      }
    } finally {
      this.lock.writeLock().unlock();
    }
  }
}
