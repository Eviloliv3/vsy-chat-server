package de.vsy.server.client_handling.data_management.access_limiter;

import de.vsy.server.client_handling.data_management.CommunicationEntityDataProvider;
import de.vsy.server.client_handling.data_management.LocalClientStateDependentLogicProvider;
import de.vsy.server.client_handling.data_management.logic.ClientStateControl;

public
interface RelationHandlingDataProvider extends BaseHandlingDataProvider {

    CommunicationEntityDataProvider getContactToActiveClientMapper ();

    ClientStateControl getGlobalClientStateControl ();

    LocalClientStateDependentLogicProvider getLocalClientStateDependentLogicProvider ();
}
