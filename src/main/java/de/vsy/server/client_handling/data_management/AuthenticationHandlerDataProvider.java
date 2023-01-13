/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.vsy.server.client_handling.data_management;

import de.vsy.server.client_handling.data_management.logic.AuthenticationStateControl;

/**
 * Provides AUTHENTICATION related Packet handlers with appropriate data access.
 */
public interface AuthenticationHandlerDataProvider extends BaseHandlingDataProvider {

    AuthenticationStateControl getAuthenticationStateControl();

    LocalClientStateObserverManager getLocalClientStateObserverManager();
}
