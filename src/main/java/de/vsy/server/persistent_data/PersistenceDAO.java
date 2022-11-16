/*
 *
 */
package de.vsy.server.persistent_data;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static java.util.Arrays.asList;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.FileLockInterruptionException;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Deserializes JSON-Strings using preset JavaTypes with the Jackson library ObjectMapper. Works on
 * a set of this.fileChannels (main and backup version of the same file).
 */
public class PersistenceDAO {

  private static final String NO_DATA_FILE_PATHS_SET;
  private static final String NO_FILE_ACCESS_ACQUIRED;
  private static final Logger LOGGER;

  static {
    LOGGER = LogManager.getLogger();
    NO_DATA_FILE_PATHS_SET = "Noch kein Dateizugriff möglich: Referenzen ueber "
        + "korrekte createFileReferences({id})-Methode erstellen.";
    NO_FILE_ACCESS_ACQUIRED = "Kein Dateizugriff möglich: Zugriffsrecht ueber "
        + "_acquireAccess()_-Metohode erlangen.";
  }

  private final JavaType dataFormat;
  private final PersistentDataFileCreator.DataFileDescriptor fileDescriptor;
  private final ReentrantReadWriteLock localLock;
  private final Lock accessLock;
  private final ObjectMapper mapper;
  private FileLock globalLock;
  private Path lockFilePath;
  private Path[] filePaths;

  /**
   * Instantiates a new specialized reader base.
   *
   * @param fileDescriptor the file descriptor
   * @param dataFormat     the dataManagement format
   */
  public PersistenceDAO(final PersistentDataFileCreator.DataFileDescriptor fileDescriptor,
      final JavaType dataFormat) {
    this.fileDescriptor = fileDescriptor;
    this.dataFormat = dataFormat;
    this.mapper = new ObjectMapper();
    this.lockFilePath = null;
    this.localLock = new ReentrantReadWriteLock();
    this.accessLock = new ReentrantLock();
    this.globalLock = null;
    this.filePaths = null;
    this.mapper.configure(INDENT_OUTPUT, true);
    this.mapper.findAndRegisterModules();
  }

  /**
   * Erstellt ein RandomAccessFile Objekt fuer bestehenden lockFilePath mit Lesbefugnissen und
   * laesst anschliessend acquireFileLock() den exklusiven Zugriff akquirieren
   *
   * @return true, wenn exklusiver Zugriff auf Daten erlangt wurde, false sonst. Zusaetzlich wird
   * das Interrupt-Flag gesetzt, wenn: lockFilePath keinen gueltigen Dateipfad enthaelt, es zu einer
   * FileLockInterruption kommt, eine allgemeine Interruption geworfen wird oder eine unerwartete
   * IOException geworfen wird.
   */
  public boolean acquireAccess(final boolean writeAccess) {
    if (this.filePaths == null) {
      throw new IllegalStateException(NO_DATA_FILE_PATHS_SET);
    }
    var accessMode = "r";
    FileChannel lockChannel;
    if (writeAccess) {
      accessMode += "w";
    }

    try {
      lockChannel = new RandomAccessFile(this.lockFilePath.toFile(), accessMode).getChannel();
      return acquireFileLock(lockChannel, writeAccess);
    } catch (final FileNotFoundException fnfe) {
      Thread.currentThread().interrupt();
      LOGGER.error("File not found: {}\n{}", lockFilePath, asList(fnfe.getStackTrace()));
    } catch (final FileLockInterruptionException flie) {
      Thread.currentThread().interrupt();
      LOGGER.info("FileLock could not be acquired due to thread interruption.\n{}",
          asList(flie.getStackTrace()));
    } catch (final ClosedChannelException cce) {
      LOGGER.error("FileLock could not be acquired. FileChannel closed.");
    } catch (IOException ioe) {
      Thread.currentThread().interrupt();
      LOGGER.error("Unexpected error during lock acquisition: {}.\nSource: {}", ioe.getMessage(),
          asList(ioe.getStackTrace()));
    }
    return false;
  }

  /**
   * Acquire file lock.
   *
   * @param toLock the channel to be locked
   * @throws ClosedChannelException        Signals that the underlying channel has been closed.
   * @throws FileLockInterruptionException Signals that thread has been interrupted while waiting
   *                                       for lock().
   */
  private boolean acquireFileLock(final FileChannel toLock, final boolean writeAccess)
      throws IOException {
    boolean accessAcquired;

    if (writeAccess) {
      accessAcquired = acquireLocalLock(this.localLock.writeLock()::tryLock);
    } else {
      accessAcquired = acquireLocalLock(this.localLock.readLock()::tryLock);
    }

    if (accessAcquired) {

      while (this.globalLock == null && !Thread.currentThread().isInterrupted()) {
        try {
          this.globalLock = toLock.lock(0, Long.MAX_VALUE, !writeAccess);
        } catch (OverlappingFileLockException ex) {
          LOGGER.trace("File still locked externally. Will be attempted again {}: {}",
              ex.getClass().getSimpleName(), ex.getMessage());
        } catch (FileLockInterruptionException fie) {
          Thread.currentThread().interrupt();
          LOGGER.error("Interrupted during global lock acquisition.");
          accessAcquired = false;
        }
      }
    }

    if (!accessAcquired || this.globalLock == null || !this.globalLock.isValid()
        || Thread.currentThread().isInterrupted()) {
      LOGGER.error(
          "Lock acquisition failed. Write mode: {}; localLock: {}; lock: {}; interrupted: {}",
          writeAccess, accessAcquired, this.globalLock, Thread.currentThread().isInterrupted());
      releaseAccess(writeAccess);
      return false;
    } else {
      return true;
    }
  }

  private boolean acquireLocalLock(Supplier<Boolean> lockingMethod) {
    var accessAcquired = false;

    while (!accessAcquired && !Thread.currentThread().isInterrupted()) {
      try {
        accessLock.lock();

        accessAcquired = lockingMethod.get();
      } finally {
        this.accessLock.unlock();
      }
      Thread.yield();
    }

    return accessAcquired;
  }

  /**
   * Release accessLimiter.
   */
  public void releaseAccess(final boolean writeAccess) {

    try {
      this.accessLock.lock();

      if (writeAccess) {
        this.releaseWriteLock();
      } else {
        this.releaseReadLock();
      }

      if (this.localLock.getReadLockCount() == 0 && !this.localLock.isWriteLocked()) {
        releaseGlobalAccess();
      }
    } finally {
      this.accessLock.unlock();
    }
  }

  private void releaseReadLock() {

    try {
      this.accessLock.lock();

      if (this.localLock.getReadHoldCount() > 0) {
        this.localLock.readLock().unlock();
      }
    } finally {
      this.accessLock.unlock();
    }
  }

  private void releaseGlobalAccess() {
    if (this.globalLock != null) {
      try {
        final var channel = this.globalLock.channel();
        channel.close();
      } catch (IOException e) {
        LOGGER.info("Global file access could not be closed. {}\n{}",
            e.getClass().getSimpleName(),
            asList(e.getStackTrace()));
      }
      this.globalLock = null;
    }
  }

  private void releaseWriteLock() {

    try {
      this.accessLock.lock();

      if (this.localLock.isWriteLockedByCurrentThread()) {
        this.localLock.writeLock().unlock();
      }
    } finally {
      this.accessLock.unlock();
    }
  }

  /**
   * Sets the message paths.
   *
   * @throws IllegalStateException the illegal state exception
   */
  public void createFileReferences() throws IllegalStateException, InterruptedException {

    if (!fileDescriptor.isIdRequired()) {

      this.createFileLockReference();
      this.createWorkingFileReferences();
    } else {
      final var errorMessage =
          "Dateipfade konnten nicht angelegt werden. " + "Für Dateideskriptor: "
              + fileDescriptor + " wird eine Pfaderweiterung benötigt.";
      throw new IllegalStateException(errorMessage);
    }
  }

  private void createFileLockReference() throws InterruptedException {
    String directory = PersistentDataLocationCreator
        .createDirectoryPath(PersistentDataLocationCreator.DataOwnershipDescriptor.SERVER, null);
    this.createSingleFileReference(directory, this.fileDescriptor.getLockFilename());
  }

  private void createWorkingFileReferences() throws InterruptedException {
    String[] directories = PersistentDataLocationCreator
        .createDirectoryPaths(PersistentDataLocationCreator.DataOwnershipDescriptor.SERVER, null);

    if (directories != null) {
      createMultipleFileReferences(directories, this.fileDescriptor.getDataFilename());
    } else {
      throw new IllegalArgumentException("Keine gueltigen Verzeichnispfade uebergeben.");
    }
  }

  private void createSingleFileReference(final String directory, final String filename) {
    this.lockFilePath = PersistentDataFileCreator.createAndGetFilePath(directory, filename, LOGGER);

    if (this.lockFilePath == null) {
      final var errorMessage = "Kein Datei-Lock angelegt für Deskriptor: " + fileDescriptor;
      throw new IllegalStateException(errorMessage);
    }
  }

  private void createMultipleFileReferences(String[] directories, String filename) {

    this.filePaths = PersistentDataFileCreator.createAndGetFilePaths(directories, filename, LOGGER);

    if (this.filePaths == null) {
      final var errorMessage = "Keine Dateipfade angelegt für Deskriptor: " + fileDescriptor;
      throw new IllegalStateException(errorMessage);
    }
  }

  /**
   * Creates the filenames.
   *
   * @param pathExtension the path extension
   * @throws IllegalStateException the illegal state exception
   */
  public void createFileReferences(final String pathExtension)
      throws IllegalStateException, InterruptedException {

    if (fileDescriptor.isIdRequired()) {

      this.createFileLockReference(pathExtension);
      this.createWorkingFileReferences(pathExtension);
    } else {
      final var errorMessage =
          "Dateipfade konnten nicht angelegt werden. " + "Dateideskriptor: " + fileDescriptor
              + " wird keine Pfaderweiterung benötigt.";
      throw new IllegalStateException(errorMessage);
    }
  }

  private void createFileLockReference(final String pathExtension) throws InterruptedException {
    String directory = PersistentDataLocationCreator
        .createDirectoryPath(PersistentDataLocationCreator.DataOwnershipDescriptor.CLIENT,
            pathExtension);
    createSingleFileReference(directory, this.fileDescriptor.getLockFilename());
  }

  private void createWorkingFileReferences(final String pathExtension) throws InterruptedException {

    if (pathExtension != null && !pathExtension.isEmpty()) {
      String[] directories = PersistentDataLocationCreator
          .createDirectoryPaths(PersistentDataLocationCreator.DataOwnershipDescriptor.CLIENT,
              pathExtension);

      if (directories != null) {
        createMultipleFileReferences(directories, this.fileDescriptor.getDataFilename());
      } else {
        throw new IllegalArgumentException("Keine gueltigen Verzeichnispfade uebergeben.");
      }
    } else {
      final var errorMessage =
          "Dateipfade konnten nicht angelegt werden. " + "Dateideskriptor: " + fileDescriptor
              + " wird eine Pfaderweiterung benötigt.";
      throw new IllegalStateException(errorMessage);
    }
  }

  /**
   * Tries to read from file and backup file. Returns first content found. If all this.fileChannels
   * are empty, null is returned.
   *
   * @return object
   */
  public Object readData() {

    if (this.globalLock == null) {
      throw new IllegalStateException(NO_FILE_ACCESS_ACQUIRED);
    }

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

  public void removeFileReferences() {
    this.filePaths = null;
    this.lockFilePath = null;
  }

  /**
   * Overwrites file and backup file content with argument, if argument is not null.
   *
   * @param toWrite the to write
   * @return true, if successful
   * @throws IllegalStateException if no data file pathNames are set
   */
  public boolean writeData(final Object toWrite) {
    if (this.globalLock == null) {
      throw new IllegalStateException(NO_FILE_ACCESS_ACQUIRED);
    }

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
    // TODO das hier sollte in einem eigenen Objekt (statische Methode) erstellt
    // werden
    // TODO am besten komplett verschlüsselt
    try {
      return this.mapper.writerFor(this.dataFormat).writeValueAsString(toWrite);
    } catch (JsonProcessingException je) {
      LOGGER.info("{}: {}", je.getClass().getSimpleName(), je.getMessage());
    }

    return null;
  }
}
