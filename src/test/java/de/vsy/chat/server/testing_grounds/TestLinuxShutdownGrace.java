package de.vsy.chat.server.testing_grounds;

import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.concurrent.TimeUnit.SECONDS;

public class TestLinuxShutdownGrace {

    public static void main(String[] args) throws IOException {
        ServerSocket ss = new ServerSocket(8080);
        ExecutorService es = Executors.newSingleThreadExecutor();
        Thread testThread = new Thread() {
            @Override
            public void run() {
                es.shutdownNow();
                try {
                    System.out.println("Test ExecutorService shutdown expected.");
                    final var testExecutorDown = es.awaitTermination(5, SECONDS);
                    if (!testExecutorDown) {
                        LogManager.getLogger().error(
                                "Test ExecutorService unexpectedly took more than 5 seconds and may be deadlocked.");
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        Runtime.getRuntime().addShutdownHook(testThread);
        es.submit(ss::accept);
    }
}
