/**
 *
 */
package de.vsy.chat.server.testing_grounds;

import de.vsy.server.persistent_data.data_bean.CommunicatorData;
import de.vsy.server.persistent_data.server_data.CommunicatorPersistenceDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 
 */
class TestCommunicatorDAO {

  final CommunicatorPersistenceDAO dataProvider = new CommunicatorPersistenceDAO();

  @BeforeEach
  void createAccess() throws InterruptedException {
    this.dataProvider.createFileAccess();
  }

  @AfterEach
  void removeAllClients() {
    this.dataProvider.removeFileAccess();
  }

  @Test
  void addComm() {
    var newComm = CommunicatorData.valueOf(11111, 11111, "New Communicator");
    Assertions.assertTrue(this.dataProvider.addCommunicator(newComm));
  }

  @Test
  void getComm() {
    var newComm = CommunicatorData.valueOf(11111, 11111, "New Communicator");
    this.dataProvider.addCommunicator(newComm);
    Assertions.assertEquals(this.dataProvider.getCommunicatorData(11111), newComm);
  }

  @Test
  void delDataComm() {
    var newComm = CommunicatorData.valueOf(11111, 11111, "New Communicator");
    this.dataProvider.addCommunicator(newComm);
    Assertions.assertTrue(this.dataProvider.removeCommunicator(newComm));
  }

  @Test
  void delIdComm() {
    var newComm = CommunicatorData.valueOf(11111, 11111, "New Communicator");
    this.dataProvider.addCommunicator(newComm);
    Assertions.assertTrue(this.dataProvider.removeCommunicator(11111));
  }
}
