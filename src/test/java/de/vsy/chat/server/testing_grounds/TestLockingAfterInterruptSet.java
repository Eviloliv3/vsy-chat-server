package de.vsy.chat.server.testing_grounds;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestLockingAfterInterruptSet {

  @Test
  void interruptSetBeforeLock() {
    var lock = new ReentrantReadWriteLock();
    Thread.currentThread().interrupt();
    lock.writeLock().lock();
    Assertions.assertEquals(1, lock.getWriteHoldCount());
  }
}
