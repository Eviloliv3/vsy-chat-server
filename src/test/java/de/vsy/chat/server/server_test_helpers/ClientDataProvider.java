/*
 *
 */
package de.vsy.chat.server.server_test_helpers;

import de.vsy.shared_transmission.dto.CommunicatorDTO;

public interface ClientDataProvider {

    /**
     * Returns the local client's CommunicatorDTO.
     *
     * @return the client CommunicatorDTO
     */
    CommunicatorDTO getCommunicatorData();
}
