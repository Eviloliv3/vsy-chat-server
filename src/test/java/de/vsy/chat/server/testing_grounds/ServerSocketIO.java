package de.vsy.chat.server.testing_grounds;

import java.io.IOException;
import java.net.ServerSocket;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestServerSocketIO {

  @Test
  void TestServerSocketCloseIOException() throws IOException {
    ServerSocket s = new ServerSocket();
    LogManager.getLogger().debug("erste Schließung");
    s.close();
    Assertions.assertThrows(IOException.class, s::close);
  }
}
