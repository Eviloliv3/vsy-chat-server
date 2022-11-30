/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.vsy.server.client_handling.data_management.access_limiter;

import de.vsy.server.client_handling.data_management.logic.AuthenticationStateControl;

/**
 * Provides authentication PacketCategory handlers with dataManagement accessLimiter.
 */
public interface AuthenticationHandlingDataProvider extends BaseHandlingDataProvider {

  /**
   * Returns the handler buffers.
   *
   * @return the handler buffers
   */
  AuthenticationStateControl getGlobalAuthenticationStateControl();
}
