/**  */
package de.vsy.chat.server.testing_grounds;

import de.vsy.server.persistent_data.server_data.IdProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** @author Frederic Heath */
class TestIdDAO {

    final IdProvider idProvider = new IdProvider();

    @BeforeEach
    void createAccess ()
    throws InterruptedException {
        idProvider.createFileAccess();
    }

    @AfterEach
    void removeAllClients () {
        idProvider.removeFileAccess();
    }

    @Test
    void addAuthenticated () {
        int newId = idProvider.getNewId();
        Assertions.assertTrue(newId > 0);
    }
}
