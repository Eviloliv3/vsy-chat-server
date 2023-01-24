package de.vsy.chat.server.testing_grounds;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class TestSocket {
    @Test
    void testSocketPortRetention() throws IOException, InterruptedException {
        AtomicReference<Socket> socket = new AtomicReference<>();
        var server = new ServerSocket(9000);
        var thread = Executors.newSingleThreadExecutor();
        thread.execute(() -> {
            Socket connection = null;
            Instant timeout = Instant.now().plusMillis(1000);
            while (connection == null && Instant.now().isBefore(timeout)) {
                try {
                    connection = new Socket("localHost", 9000);
                } catch (IOException e) {
                    System.out.println("No connection found.");
                }
            }
            socket.set(connection);
        });
        var serverConnection = server.accept();
        Thread.sleep(100);
        server.close();
        serverConnection.close();

        Assertions.assertNull(socket.get().getChannel());
        socket.get().close();
        Assertions.assertTrue(socket.get().isClosed());
        Assertions.assertEquals(9000, socket.get().getPort());
    }
}
