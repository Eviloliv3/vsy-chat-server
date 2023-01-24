package de.vsy.chat.server.raw_server_test;

import de.vsy.chat.server.server_test_helpers.ClientConnection;
import de.vsy.shared_transmission.dto.authentication.AuthenticationDTO;
import org.junit.jupiter.api.Assertions;

public class AuthenticationHelper {
    public static ClientConnection loginSpecificClient(ClientConnection connection, final AuthenticationDTO credentials) {

        if (connection != null) {
            connection.setClientData(credentials, null);
            if (connection.tryClientLogin()) {
                return connection;
            } else {
                connection.setClientData(null, null);
            }
        } else {
            Assertions.fail("No usable connection.");
        }
        return null;
    }
}
