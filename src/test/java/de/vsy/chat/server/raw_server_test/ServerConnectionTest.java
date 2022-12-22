/**
 *
 */
package de.vsy.chat.server.raw_server_test;

import java.io.IOException;
import java.net.Socket;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 */
class ServerConnectionTest {

  @Test
  void test() throws IOException {

    for (var i = 0; i < 12; i++) {
      Socket socket;

      socket = new Socket("127.0.0.1", 7371);
      Assertions.assertNotNull(socket);
      socket.close();
      Assertions.assertTrue(socket.isConnected());
    }
  }
}
