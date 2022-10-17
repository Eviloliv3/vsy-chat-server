/**  */
package de.vsy.chat.server.testing_grounds;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.vsy.server.persistent_data.data_bean.AuthenticationData;
import de.vsy.server.persistent_data.server_data.ClientAuthPersistenceDAO;

/** @author Frederic Heath */
class TestAuthenticationDAO {

	final ClientAuthPersistenceDAO dataProvider = new ClientAuthPersistenceDAO();

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
		var newComm = AuthenticationData.valueOf("NewComm1", "login", 11111);
		Assertions.assertTrue(this.dataProvider.saveAccountData(newComm));
	}

	@Test
	void getComm() {
		var newComm = AuthenticationData.valueOf("NewComm", "login", 11111);
		this.dataProvider.saveAccountData(newComm);
		Assertions.assertEquals(11111, this.dataProvider.getClientId("NewComm", "login"));
	}
}
