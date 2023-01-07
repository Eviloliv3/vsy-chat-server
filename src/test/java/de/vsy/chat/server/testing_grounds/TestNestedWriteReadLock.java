package de.vsy.chat.server.testing_grounds;

import org.junit.jupiter.api.Test;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TestNestedWriteReadLock {

    @Test
    void testReentrantReadWriteLock() throws InterruptedException {
        var lock = new ReentrantReadWriteLock();
        lock.writeLock().lock();
        lock.readLock().lock();
    }
}
