package de.vsy.chat.server.raw_server_test;

import de.vsy.chat.server.server_test_helpers.ClientConnection;
import de.vsy.shared_transmission.dto.authentication.AuthenticationDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;

public class AuthenticationHelper {
    static Logger LOGGER = LogManager.getLogger();
    public static ClientConnection loginSpecificClient(ClientConnection connection, final AuthenticationDTO credentials) {

        if (connection != null) {
            connection.setClientData(credentials, null);
            if (connection.tryClientLogin()) {
                return connection;
            } else {
                Assertions.fail("Login failed for: " + credentials);
            }
        } else {
            Assertions.fail("No usable connection. Failed for: " + credentials);
        }
        return null;
    }

    public static void logoutClient(final ClientConnection toLogout) {
        final var clientName = toLogout.getCommunicatorData().getDisplayLabel();
        LOGGER.info("{}-logout attempt initiated.", clientName);
        final boolean logoutSuccess;

        if (toLogout.tryClientLogout()) {
            LOGGER.info("{}-logout successful.", clientName);
        }
        LOGGER.info("{}-connection terminated successfully.", clientName);
    }
}
