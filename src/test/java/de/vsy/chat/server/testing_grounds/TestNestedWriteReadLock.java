package de.vsy.chat.server.testing_grounds;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.junit.jupiter.api.Test;

public class TestNestedWriteReadLock {

  @Test
  void testReentrantReadWriteLock() throws InterruptedException {
    var lock = new ReentrantReadWriteLock();
    lock.writeLock().lock();
    lock.readLock().lock();
  }
}
