package de.vsy.chat.server.testing_grounds;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public
class TestNestedWriteReadLock {

    @Test
    void testReentrantReadWriteLock(){
        var lock = new ReentrantReadWriteLock();
        lock.writeLock().lock();
        lock.readLock().lock();
        lock.readLock().unlock();
        lock.writeLock().unlock();
        Assertions.assertEquals(0, lock.getReadLockCount());
    }
}
