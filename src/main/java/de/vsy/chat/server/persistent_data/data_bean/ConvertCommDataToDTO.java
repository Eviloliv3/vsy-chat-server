package de.vsy.chat.server.persistent_data.data_bean;

import de.vsy.chat.shared_transmission.dto.CommunicatorDTO;

import static de.vsy.chat.shared_transmission.dto.CommunicatorDTO.valueOf;
import static de.vsy.chat.shared_transmission.dto.standard_empty_value.StandardEmptyDataProvider.EMPTY_COMMUNICATOR;

/**
 * The Class ConvertCommDataToDTO.
 *
 * @author Frederic Heath
 */
public
class ConvertCommDataToDTO {

    /** Instantiates a new convert comm dataManagement to entity. */
    private
    ConvertCommDataToDTO () {
    }

    /**
     * Convert from.
     *
     * @param commData the comm dataManagement
     *
     * @return the communication entity
     */
    public static
    CommunicatorDTO convertFrom (final CommunicatorData commData) {
        CommunicatorDTO communicatorData;

        if (commData != null) {
            communicatorData = valueOf(commData.getCommunicatorId(),
                                       commData.getDisplayName());
        } else {
            communicatorData = EMPTY_COMMUNICATOR;
        }
        return communicatorData;
    }
}
