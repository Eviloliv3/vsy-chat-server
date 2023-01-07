package de.vsy.chat.server.testing_grounds;

import org.junit.jupiter.api.Test;

import java.time.Instant;

public class ThreadInterruptionTest implements Runnable {

    @Override
    public void run() {

        while (!Thread.currentThread().isInterrupted()) {
            Thread.yield();
        }
    }

    @Test
    void TestThreadInterruption() throws InterruptedException {
        var maxWait = 500;
        var testThread = new Thread(new ThreadInterruptionTest());
        testThread.start();

        var endTime = Instant.now().plusMillis(5000);

        testThread.interrupt();

        do {
            Thread.yield();
            if (Instant.now().isAfter(endTime)) {
                break;
            }
        } while (testThread.isAlive());

        if (!testThread.isAlive()) {
        }
        testThread.join();
    }
}
