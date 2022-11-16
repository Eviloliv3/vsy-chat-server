/**
 *
 */
package de.vsy.chat.server.testing_grounds;

import static de.vsy.server.persistent_data.server_data.temporal.IdType.CLIENT;
import static de.vsy.server.persistent_data.server_data.temporal.IdType.GROUP;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vsy.server.persistent_data.server_data.IdProvider;
import de.vsy.server.persistent_data.server_data.temporal.IdProviderPool;
import java.util.LinkedList;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Frederic Heath
 */
class TestIdDAO {

  final IdProvider idProvider = new IdProvider();

  @BeforeEach
  void createAccess() throws InterruptedException {
    idProvider.createFileAccess();
  }

  @AfterEach
  void removeAllClients() {
    idProvider.removeFileAccess();
  }

  @Test
  void addAuthenticated() {
    int newId = idProvider.getNewId(CLIENT);
    System.out.println("id");
    Assertions.assertTrue(newId > 0);
  }

  @Test
  void serializeIdPoolProvider() throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    IdProviderPool p = IdProviderPool.instantiate(Map.of(CLIENT, 135, GROUP, 123),
        Map.of(CLIENT, new LinkedList<>(), GROUP, new LinkedList<>()));
    String jackedp = mapper.writerFor(IdProviderPool.class).writeValueAsString(p);
    System.out.println(jackedp);
    p = mapper.readValue(jackedp, IdProviderPool.class);
    Assertions.assertEquals(p, p);
  }
}
