package de.vsy.chat.server.testing_grounds;

import de.vsy.shared_transmission.packet.Packet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

public class TestBlockingDeque {

    @Test
    void testChangingPolledDeque() throws InterruptedException {
        QueueWrapper queue = new QueueWrapper();
        Runnable readThread = () -> queue.read();
        Runnable queueChangeThread = () -> queue.changeQueue();
        ExecutorService s = Executors.newFixedThreadPool(2);
        s.execute(readThread);
        s.execute(queueChangeThread);
        Thread.sleep(100);
        Assertions.assertTrue(queue.queueChanged);
    }

    class QueueWrapper {

        private BlockingDeque<Packet> queue = new LinkedBlockingDeque<>();
        private boolean queueChanged = false;

        void read() {
            try {
                System.out.println(queue.poll(100, TimeUnit.MICROSECONDS));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        void changeQueue() {
            queue = new LinkedBlockingDeque<>();
            queueChanged = true;
        }
    }
}
