/*
 *
 */
package de.vsy.chat.server.server_test_helpers;

import de.vsy.shared_transmission.shared_transmission.dto.CommunicatorDTO;

/**
 * Interface for simple client dataManagement accessLimiter throughout the application.
 */
public interface ClientDataProvider {

  /**
   * Gets the client dataManagement.
   *
   * @return the client dataManagement
   */
  CommunicatorDTO getCommunicatorData();
}
