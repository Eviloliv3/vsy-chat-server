/*
 *
 */
package de.vsy.server.persistent_data;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.FileLockInterruptionException;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static java.util.Arrays.asList;

/**
 * Deserializes JSON-Strings using preset JavaTypes with the Jackson library ObjectMapper. Works on
 * a set of fileChannels (main and backup version of the same file).
 */
public class SynchronousFileManipulator implements PersistentDataAccessProvider {

    private static final String NO_DATA_FILE_PATHS_SET;
    private static final String NO_FILE_ACCESS_ACQUIRED;
    private static final Logger LOGGER;

    static {
        LOGGER = LogManager.getLogger();
        NO_DATA_FILE_PATHS_SET = "File access not possible: create references using createFileReferences({id}) methods.";
        NO_FILE_ACCESS_ACQUIRED = "File access not possible: acquire access rights using _acquireAccess()_.";
    }

    private final JavaType dataFormat;
    private final ReentrantReadWriteLock localLock;
    private final Condition referenceRemovalCondition;
    private final Lock accessLock;
    private final ObjectMapper mapper;
    private final Map<Path, FileLock> globalLocks;
    private final List<Thread> threadsWithValidLock;
    private Path[] filePaths;

    /**
     * Instantiates a new specialized reader base.
     *
     * @param dataFormat the dataManagement format
     */
    public SynchronousFileManipulator(final JavaType dataFormat) {
        this.dataFormat = dataFormat;
        this.mapper = new ObjectMapper();
        this.localLock = new ReentrantReadWriteLock();
        this.accessLock = new ReentrantLock();
        this.referenceRemovalCondition = this.accessLock.newCondition();
        this.globalLocks = new HashMap<>();
        this.threadsWithValidLock = new LinkedList<>();
        this.filePaths = null;
        this.mapper.configure(INDENT_OUTPUT, true);
        this.mapper.findAndRegisterModules();
    }

    /**
     * Tries to acquire locks on local lock and FileLocks per managed working file.
     * Interrupt flag is set, if interrupted while acquiring file locks or no working
     * file exists.
     *
     * @return true, if exclusive access could be acquired, false otherwise.
     * @throws IllegalStateException if no file paths were previously set
     */
    public boolean acquireAccess(final boolean sharedAccess) {
        if (this.filePaths == null) {
            throw new IllegalStateException(NO_DATA_FILE_PATHS_SET);
        }
        final var localLockAcquired = acquireLocalLock(sharedAccess);

        if (localLockAcquired) {
            final boolean globalLockAcquired;

            if (sharedAccess) {
                this.accessLock.lock();

                try {
                    globalLockAcquired = acquireGlobalLocks(true);
                } finally {
                    this.accessLock.unlock();
                }
            } else {
                globalLockAcquired = acquireGlobalLocks(false);
            }

            if (!globalLockAcquired) {
                LOGGER.error("Global locks not acquired. All locks will be released.");
                releaseAccess(sharedAccess);
                return false;
            }
            addThread(Thread.currentThread());
            return true;
        } else {
            LOGGER.error("Interruption prevented local lock acquisition.");
        }
        return false;
    }

    /**
     * Acquire file locks.
     */
    private boolean acquireGlobalLocks(final boolean sharedAccess) {
        OpenOption fileMode = sharedAccess ? StandardOpenOption.READ : StandardOpenOption.WRITE;
        int validFileCounter = this.filePaths.length;

        for (final var workingFilePath : this.filePaths) {
            if (this.globalLocks.containsKey(workingFilePath)) {
                continue;
            }
            FileLock workingFileLock = null;
            final var workingFileChannel = getFileChannel(workingFilePath, fileMode);

            if (workingFileChannel != null) {
                workingFileLock = getFileLock(workingFileChannel, sharedAccess);
            } else {
                validFileCounter--;

                if (validFileCounter == 0) {
                    LOGGER.error("No existing working files found. Interrupt flag set.");
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            if (workingFileLock != null) {
                this.globalLocks.put(workingFilePath, workingFileLock);
            }
        }
        return this.globalLocks.size() == validFileCounter;
    }

    private FileChannel getFileChannel(Path filePath, OpenOption... options) {
        FileChannel openedChannel;

        try {
            openedChannel = FileChannel.open(filePath, options);
        } catch (FileNotFoundException fnfe) {
            LOGGER.error("File not found for path: {}", filePath);
            openedChannel = null;
        } catch (IOException ioe) {
            LOGGER.error("No FileChannel for path: {}\n{}", filePath, Arrays.asList(ioe.getStackTrace()));
            openedChannel = null;
        }
        return openedChannel;
    }

    private FileLock getFileLock(final FileChannel channel, final boolean sharedAccess) {
        FileLock workingFileLock = null;

        while (workingFileLock == null && !(Thread.currentThread().isInterrupted())) {
            try {
                workingFileLock = channel.lock(0, Long.MAX_VALUE, sharedAccess);
            } catch (OverlappingFileLockException ex) {
                LOGGER.trace("File still locked externally. Will be attempted again {}: {}",
                        ex.getClass().getSimpleName(), ex.getMessage());
            } catch (FileLockInterruptionException fie) {
                Thread.currentThread().interrupt();
                LOGGER.error("Interrupted during global lock acquisition.");
                return null;
            } catch (IOException ioe) {
                LOGGER.error("No FileLock for channel: {}\n{}", channel, Arrays.asList(ioe.getStackTrace()));
                return null;
            }
            Thread.yield();
        }
        return workingFileLock;
    }

    private boolean acquireLocalLock(final boolean sharedAccess) {
        var accessAcquisitionState = false;
        final Supplier<Boolean> lockingMethod;

        if (sharedAccess) {
            lockingMethod = this.localLock.readLock()::tryLock;
        } else {
            lockingMethod = this.localLock.writeLock()::tryLock;
        }

        while (!accessAcquisitionState && !Thread.currentThread().isInterrupted()) {
            this.accessLock.lock();

            try {
                accessAcquisitionState = lockingMethod.get();
            } finally {
                this.accessLock.unlock();
            }
            Thread.yield();
        }
        return accessAcquisitionState;
    }

    private void checkThreadAccess() {
        this.accessLock.lock();

        try {
            if (!(this.threadsWithValidLock.contains(Thread.currentThread()))) {
                throw new IllegalStateException(NO_FILE_ACCESS_ACQUIRED);
            }
        } finally {
            this.accessLock.unlock();
        }
    }

    /**
     * Release accessLimiter.
     *
     * @throws IllegalStateException if thread did not previously acquire access to
     *                               files using acquireAccess(...)
     */
    public void releaseAccess(boolean sharedAccess) {
        checkThreadAccess();

        if (sharedAccess) {
            this.accessLock.lock();

            try {
                if (this.localLock.getReadLockCount() == 1 && !(this.localLock.isWriteLocked())) {
                    releaseGlobalAccess();
                }
                this.localLock.readLock().unlock();
                removeThread(Thread.currentThread());
            } finally {
                this.accessLock.unlock();
            }
        } else if (this.localLock.isWriteLockedByCurrentThread()) {
            if (this.localLock.getWriteHoldCount() == 1) {
                releaseGlobalAccess();
            }
            this.localLock.writeLock().unlock();
            removeThread(Thread.currentThread());
        } else {
            LOGGER.warn("Attempted to illegally remove exclusive file access.");
        }
    }

    private void releaseGlobalAccess() {
        for (final var filePath : this.filePaths) {
            final var lock = this.globalLocks.get(filePath);

            try {
                lock.release();
            } catch (IOException e) {
                LOGGER.error("Could not release FileLock for path: {}", filePath);
            }
            this.globalLocks.remove(filePath);
        }
    }

    private void addThread(final Thread threadWithLock) {
        this.accessLock.lock();

        try {
            this.threadsWithValidLock.add(threadWithLock);
        } finally {
            this.accessLock.unlock();
        }
    }

    public void deleteFiles() {
        if (this.filePaths == null) {
            this.accessLock.lock();
            try {
                removeFileReferences();
                PersistentDataLocationRemover.deleteDirectories(filePaths, LOGGER);
            } finally {
                this.accessLock.unlock();
            }

        }
    }

    private boolean removeThread(final Thread threadWithoutLock) {
        this.accessLock.lock();

        try {
            var threadRemoved = this.threadsWithValidLock.remove(threadWithoutLock);

            if (this.threadsWithValidLock.isEmpty()) {
                this.referenceRemovalCondition.signal();
            }
            return threadRemoved;
        } finally {
            this.accessLock.unlock();
        }
    }

    /**
     * Sets the message paths.
     *
     * @throws IllegalStateException if no any of the specified file paths is null.
     */
    public void setFilePaths(Path[] filePaths) throws IllegalStateException {
        if (filePaths == null) {
            throw new IllegalArgumentException("File paths specified.");
        }

        for (Path filePath : filePaths) {
            if (filePath == null) {
                throw new IllegalArgumentException("Illegal null path found.");
            }
        }
        this.filePaths = filePaths;
    }

    /**
     * Tries to read from file and backup file. Returns first content specified. If all
     * this.fileChannels are empty, null is returned.
     *
     * @return object
     * @throws IllegalStateException if thread did not previously acquire access to
     *                               files using acquireAccess(...)
     */
    public Object readData() {
        checkThreadAccess();

        Object readObject = null;
        final var lastChangedFile = this.getLatestChangedFilePath();

        try {
            String readJsonString = Files.readString(lastChangedFile);
            readObject = mapper.readValue(readJsonString, dataFormat);
        } catch (JsonParseException | JsonMappingException je) {
            LOGGER.info("Read data could not be instantiated: {}\n{}: {}", lastChangedFile,
                    je.getClass().getSimpleName(), je.getMessage());
        } catch (final FileNotFoundException ex) {
            Thread.currentThread().interrupt();
            LOGGER.info("File could not found: {}\n{}", lastChangedFile, asList(ex.getStackTrace()));
        } catch (final IOException ex) {
            Thread.currentThread().interrupt();
            LOGGER.info("Reading from file failed: {}\n{}", lastChangedFile,
                    asList(ex.getStackTrace()));
        }
        return readObject;
    }

    private Path getLatestChangedFilePath() {
        Path upToDateFile = null;
        Instant lastChangedTime = Instant.MIN;

        for (final var currentFile : this.filePaths) {
            Instant modificationTime = Instant.ofEpochMilli(currentFile.toFile().lastModified());

            if (modificationTime.isAfter(lastChangedTime)) {
                lastChangedTime = modificationTime;
                upToDateFile = currentFile;
            }
        }
        return upToDateFile;
    }

    public boolean removeFileReferences() {
        this.accessLock.lock();

        try {
            if (!(this.threadsWithValidLock.isEmpty())) {
                try {
                    this.referenceRemovalCondition.await();
                } catch (InterruptedException e) {
                    LOGGER.error("Interrupted while waiting for threads to release locks.");
                    return false;
                }
            }
            this.filePaths = null;
            return true;
        } finally {
            this.accessLock.unlock();
        }
    }

    /**
     * Overwrites file and backup file content with argument, if argument is not null.
     *
     * @param toWrite the to write
     * @return true, if successful
     * @throws IllegalStateException if thread did not previously acquire access to
     *                               files using acquireAccess(...)
     */
    public boolean writeData(final Object toWrite) {
        checkThreadAccess();

        boolean dataWritten = true;
        final var jsonString = generateJsonFromObject(toWrite);

        if (jsonString != null) {

            for (final var currentPath : this.filePaths) {
                try {
                    Files.writeString(currentPath, jsonString);
                } catch (final IOException e) {
                    Thread.currentThread().interrupt();
                    LOGGER.info("Could not write to {}." + "\n{}", currentPath,
                            asList(e.getStackTrace()));
                    return !dataWritten;
                }
            }
            return dataWritten;
        } else {
            LOGGER.error("JSON-string could not be created from: {}", toWrite);
        }
        return !dataWritten;
    }

    private String generateJsonFromObject(final Object toWrite) {
        //TODO should be outsourced to object with static access and encrypted
        try {
            return this.mapper.writerFor(this.dataFormat).writeValueAsString(toWrite);
        } catch (JsonProcessingException je) {
            LOGGER.info("{}: {}", je.getClass().getSimpleName(), je.getMessage());
        }
        return null;
    }
}
