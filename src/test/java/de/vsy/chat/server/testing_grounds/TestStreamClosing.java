package de.vsy.chat.server.testing_grounds;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestStreamClosing {

	@Test
	void testStreamClosing() {
		try (var s = new ServerSocket(8000)) {
			var test = newSingleThreadExecutor();

			var verbindung = test.submit(s::accept);
			var socket = new Socket("127.0.0.1", 8000);
			var serverIn = verbindung.get();

			serverIn.getInputStream();
			new ObjectOutputStream(socket.getOutputStream());
			socket.close();
		} catch (IOException | ExecutionException | InterruptedException e) {
			Assertions.fail(e);
		}
	}
}
