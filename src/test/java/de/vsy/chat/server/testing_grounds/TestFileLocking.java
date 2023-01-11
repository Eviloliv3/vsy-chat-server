package de.vsy.chat.server.testing_grounds;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.FileLockInterruptionException;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestFileLocking
{
    Logger LOGGER = LogManager.getLogger();
    Map<Path, FileLock> locks = new HashMap<>();
    @Test
    void testReentrantFileLocking() throws IOException, InterruptedException {
        ExecutorService ex = Executors.newFixedThreadPool(2);
        final var file = System.getProperty("user.home") + File.separator + "testFile.lock";
        final var path = Path.of(file);
        if(!path.toFile().isFile()){
            path.toFile().createNewFile();
        }
        var lock1 = getLock(path, true, StandardOpenOption.READ);
        Assertions.assertTrue(lock1.isValid());
        CountDownLatch l = new CountDownLatch(1);
        ex.execute(() -> {
            var buffer = ByteBuffer.allocate(1024);
            while(l.getCount() > 0) {
                try {
                    lock1.channel().read(buffer);
                    LOGGER.error("1 Read successfully.");
                    buffer.clear();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        Thread.sleep(100);
        var lock2 = getLock(path, true, StandardOpenOption.READ);
        ex.execute(() -> {
            var buffer = ByteBuffer.allocate(1024);
            try {
                lock2.channel().read(buffer);
                LOGGER.error("2 Read successfully.");
                buffer.clear();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }finally {
                l.countDown();
            }
        });
    }
    private FileLock getLock(final Path path, boolean sharedAccess, OpenOption... s){
        return locks.computeIfAbsent(path, key -> {
            while(!Thread.currentThread().isInterrupted()) {
                try {
                    final var channel = FileChannel.open(key, s);
                    return channel.lock(0L, Long.MAX_VALUE, sharedAccess);
                } catch (OverlappingFileLockException ofle) {
                    LOGGER.trace("File still locked externally. Will be attempted again {}: {}",
                            ofle.getClass().getSimpleName(), ofle.getMessage());
                } catch (FileLockInterruptionException fie) {
                    Thread.currentThread().interrupt();
                    LOGGER.error("Interrupted during global lock acquisition.");
                    return null;
                } catch (IOException e) {
                    LOGGER.error(e);
                    return null;
                }
            }
            return null;
        });
    }
}
