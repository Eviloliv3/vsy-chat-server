/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.vsy.server.client_handling.data_management;

import de.vsy.server.client_handling.data_management.access_limiter.*;
import de.vsy.server.client_handling.data_management.bean.ClientDataManager;
import de.vsy.server.client_handling.data_management.bean.ClientStateManager;
import de.vsy.server.client_handling.data_management.bean.LocalClientDataProvider;
import de.vsy.server.client_handling.data_management.bean.LocalClientStateProvider;
import de.vsy.server.client_handling.data_management.logic.AuthenticationStateControl;
import de.vsy.server.client_handling.data_management.logic.ClientStateControl;
import de.vsy.server.client_handling.data_management.logic.ClientStateRecorder;
import de.vsy.server.client_handling.packet_processing.processor.ResultingPacketCreator;
import de.vsy.server.server.data.access.HandlerAccessManager;
import de.vsy.server.server_packet.dispatching.PacketTransmissionCache;
import de.vsy.server.server_packet.packet_creation.ClientHandlerPacketCreator;
import de.vsy.server.server_packet.packet_creation.ResultingPacketContentHandler;
import de.vsy.shared_module.shared_module.packet_management.PacketBuffer;
import de.vsy.shared_module.shared_module.packet_management.ThreadPacketBufferLabel;
import de.vsy.shared_module.shared_module.packet_management.ThreadPacketBufferManager;

/**
 * Manages most dataManagement and dataManagement accessLimiter granting objects
 * concerning a ClientConnectionHandler
 */
@SuppressWarnings("used to share multiple mutable objects throughout ClientConnectionHandler ")
public final
class HandlerLocalDataManager
        implements AuthenticationHandlingDataProvider, StatusHandlingDataProvider, ErrorHandlingDataProvider, RelationHandlingDataProvider, ChatHandlingDataProvider {

    private final PacketTransmissionCache cachedPackets;
    private final ClientDataManager clientDataManager;
    private final ClientStateManager clientStateManager;
    private final CommunicationEntityDataProvider contactMapper;
    private final ThreadPacketBufferManager threadBuffers;
    private final LocalClientStateDependentLogicProvider stateDependingAccess;
    private final ClientStateRecorder stateRecorder;
    private final ResultingPacketContentHandler contentHandler;
    private final ResultingPacketCreator packetCreator;

    /**
     * Instantiates a new client thread related dataManagement manager.
     *
     * @param requestAssignmentBuffer the request assignment buffer
     */
    public
    HandlerLocalDataManager (final PacketBuffer requestAssignmentBuffer) {
        this.threadBuffers = new ThreadPacketBufferManager();
        this.clientDataManager = new ClientDataManager();
        this.clientStateManager = new ClientStateManager();
        this.cachedPackets = new PacketTransmissionCache();
        this.packetCreator = new ClientHandlerPacketCreator(this.clientDataManager);
        this.contentHandler = new ResultingPacketContentHandler(packetCreator,
                                                                cachedPackets);
        this.stateRecorder = new ClientStateRecorder(this.clientStateManager,
                                                     this.clientDataManager);
        this.contactMapper = new CommunicationEntityDataProvider(
                HandlerAccessManager.getClientSubscriptionManager(),
                HandlerAccessManager.getCommunicatorDataManipulator());
        createThreadPacketBuffers(requestAssignmentBuffer);
        this.stateDependingAccess = new LocalClientStateDependentLogicProvider(this);
    }

    /**
     * Creates the thread PacketBuffers.
     *
     * @param requestAssignmentBuffer the request assignment buffer
     */
    private
    void createThreadPacketBuffers (PacketBuffer requestAssignmentBuffer) {
        this.threadBuffers.setPacketBuffer(ThreadPacketBufferLabel.SERVER_BOUND,
                                           requestAssignmentBuffer);
        this.threadBuffers.registerPacketBuffer(
                ThreadPacketBufferLabel.OUTSIDE_BOUND);
        this.threadBuffers.registerPacketBuffer(
                ThreadPacketBufferLabel.HANDLER_BOUND);
    }

    @Override
    public
    AuthenticationStateControl getGlobalAuthenticationStateControl () {
        return this.stateRecorder;
    }

    @Override
    public
    ResultingPacketContentHandler getResultingPacketContentHandler () {
        return this.contentHandler;
    }

    @Override
    public
    LocalClientDataProvider getLocalClientDataProvider () {
        return this.clientDataManager;
    }

    @Override
    public
    LocalClientStateProvider getLocalClientStateProvider () {
        return this.clientStateManager;
    }

    public
    ClientStateManager getClientStateManager () {
        return this.clientStateManager;
    }

    public
    ThreadPacketBufferManager getHandlerBufferManager () {
        return this.threadBuffers;
    }

    @Override
    public
    ClientStateControl getGlobalClientStateControl () {
        return this.stateRecorder;
    }

    @Override
    public
    CommunicationEntityDataProvider getContactToActiveClientMapper () {
        return this.contactMapper;
    }

    @Override
    public
    LocalClientStateDependentLogicProvider getLocalClientStateDependentLogicProvider () {
        return this.stateDependingAccess;
    }

    public
    PacketTransmissionCache getPacketTransmissionCache () {
        return this.cachedPackets;
    }

    public
    ResultingPacketCreator getResultingPacketCreator () {
        return this.packetCreator;
    }
}
