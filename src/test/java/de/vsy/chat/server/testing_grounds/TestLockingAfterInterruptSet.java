package de.vsy.chat.server.testing_grounds;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TestLockingAfterInterruptSet {

    @Test
    void interruptSetBeforeLock() {
        var lock = new ReentrantReadWriteLock();
        Thread.currentThread().interrupt();
        lock.writeLock().lock();
        Assertions.assertEquals(1, lock.getWriteHoldCount());
    }
}
