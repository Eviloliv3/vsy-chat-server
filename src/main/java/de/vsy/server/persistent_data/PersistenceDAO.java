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
import java.io.RandomAccessFile;
import java.nio.channels.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static java.lang.Integer.MAX_VALUE;
import static java.util.Arrays.asList;

/**
 * Deserializes JSON-Strings using preset JavaTypes with the Jackson library
 * ObjectMapper. Works on a set of this.fileChannels (main and backup version of the
 * same file).
 */
public
class PersistenceDAO {

    private static final String NO_DATA_FILE_PATHS_SET;
    private static final String NO_FILE_ACCESS_ACQUIRED;
    private static final Logger LOGGER;
    private final JavaType dataFormat;
    private final PersistentDataFileCreator.DataFileDescriptor fileDescriptor;
    private final ReadWriteLock localLock;
    private final ObjectMapper mapper;
    private Path lockFilePath;
    private FileLock globalLock;
    private Path[] filePaths;

    static {
        LOGGER = LogManager.getLogger();
        NO_DATA_FILE_PATHS_SET = "Noch kein Dateizugriff möglich: Referenzen ueber " +
                                 "korrekte createFileReferences({id})-Methode erstellen.";
        NO_FILE_ACCESS_ACQUIRED = "Kein Dateizugriff möglich: Zugriffsrecht ueber " +
                                  "_acquireAccess()_-Metohode erlangen.";
    }

    /**
     * Instantiates a new specialized reader base.
     *
     * @param fileDescriptor the file descriptor
     * @param dataFormat the dataManagement format
     */
    public
    PersistenceDAO (
            final PersistentDataFileCreator.DataFileDescriptor fileDescriptor,
            final JavaType dataFormat) {
        this.fileDescriptor = fileDescriptor;
        this.dataFormat = dataFormat;
        this.mapper = new ObjectMapper();
        this.lockFilePath = null;
        this.globalLock = null;
        this.localLock = new ReentrantReadWriteLock();
        this.filePaths = null;
        this.mapper.configure(INDENT_OUTPUT, true);
        this.mapper.findAndRegisterModules();
    }

    /**
     * Acquire accessLimiter.
     *
     * @param writeAccess the write accessLimiter
     */
    public
    boolean acquireAccess (final boolean writeAccess) {
        if (this.filePaths == null) {
            throw new IllegalStateException(NO_DATA_FILE_PATHS_SET);
        }
        FileChannel lockChannel;
        String accessModifiers = "r";

        if (writeAccess) {
            accessModifiers += "w";
        }

        try {
            lockChannel = new RandomAccessFile(this.lockFilePath.toFile(),
                                               accessModifiers).getChannel();
            return acquireFileLock(lockChannel, !writeAccess);
        } catch (final FileNotFoundException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("Datei nicht gefunden: {}\n{}", lockFilePath,
                         asList(e.getStackTrace()));
        } catch (final FileLockInterruptionException e) {
            Thread.currentThread().interrupt();
            LOGGER.info(
                    "FileLock konnte nicht akquiriert werden, da der Thread unterbrochen wurde.\n{}",
                    asList(e.getStackTrace()));
        } catch (final ClosedChannelException e) {
            LOGGER.error(
                    "FileLock konnte nicht akquiriert werden, da der FileChannel geschlossen wurde.");
        }
        return false;
    }

    /**
     * Acquire file lock.
     *
     * @param toLock the channel to be locked
     * @param sharedAccess the shared accessLimiter
     *
     * @throws ClosedChannelException        Signals that the underlying channel has
     *                                       been closed.
     * @throws FileLockInterruptionException Signals that thread has been interrupted
     *                                       while waiting for lock().
     */
    private
    boolean acquireFileLock (final FileChannel toLock, final boolean sharedAccess)
    throws ClosedChannelException, FileLockInterruptionException {
        try{
            if (sharedAccess) {
                this.localLock.readLock().lockInterruptibly();
            } else {
                this.localLock.writeLock().lockInterruptibly();
            }
        }catch(InterruptedException ie){
            Thread.currentThread().interrupt();
            LOGGER.error("Beim holen des Locks unterbrochen. {}", asList(ie.getStackTrace()));
            return false;
        }

        while (globalLock == null && !Thread.currentThread().isInterrupted()) {
            try {
                this.globalLock = toLock.tryLock(0L, MAX_VALUE, sharedAccess);
            } catch (ClosedChannelException |
                     FileLockInterruptionException killException) {
                throw killException;
            } catch (OverlappingFileLockException | IOException ex) {
                LOGGER.info("Datei noch gesperrt. Neuer Versuch wird " +
                            "gestartet. {}: {}", ex.getClass().getSimpleName(),
                            ex.getMessage());
            }
        }
        LOGGER.error(" lockNOTNull: {}; isValid: {}", globalLock, globalLock.isValid());
        return globalLock != null && globalLock.isValid();
    }

    /**
     * Check for active lock.
     *
     * @return true, if successful
     */
    public
    boolean checkForActiveLock () {
        return (this.globalLock != null) && (this.globalLock.isValid());
    }

    /**
     * Sets the message paths.
     *
     * @throws IllegalStateException the illegal state exception
     */
    public
    void createFileReferences ()
    throws IllegalStateException, InterruptedException {

        if (!fileDescriptor.isIdRequired()) {

            this.createFileLockReference();
            this.createWorkingFileReferences();
        } else {
            final var errorMessage = "Dateipfade konnten nicht angelegt werden. " +
                                     "Für Dateideskriptor: " + fileDescriptor +
                                     " wird eine Pfaderweiterung benötigt.";
            throw new IllegalStateException(errorMessage);
        }
    }

    private
    void createFileLockReference ()
    throws InterruptedException {
        String directory = PersistentDataLocationCreator.createDirectoryPath(
                PersistentDataLocationCreator.DataOwnershipDescriptor.SERVER, null);
        this.createSingleFileReference(directory,
                                       this.fileDescriptor.getLockFilename());
    }

    private
    void createWorkingFileReferences ()
    throws InterruptedException {
        String[] directories = PersistentDataLocationCreator.createDirectoryPaths(
                PersistentDataLocationCreator.DataOwnershipDescriptor.SERVER, null);

        if (directories != null) {
            createMultipleFileReferences(directories,
                                         this.fileDescriptor.getDataFilename());
        } else {
            throw new IllegalArgumentException(
                    "Keine gueltigen Verzeichnispfade uebergeben.");
        }
    }

    private
    void createSingleFileReference (final String directory, final String filename) {
        this.lockFilePath = PersistentDataFileCreator.createAndGetFilePath(directory,
                                                                           filename,
                                                                           LOGGER);

        if (this.lockFilePath == null) {
            final var errorMessage =
                    "Kein Datei-Lock angelegt für Deskriptor: " + fileDescriptor;
            throw new IllegalStateException(errorMessage);
        }
    }

    private
    void createMultipleFileReferences (String[] directories, String filename) {

        this.filePaths = PersistentDataFileCreator.createAndGetFilePaths(directories,
                                                                         filename,
                                                                         LOGGER);

        if (this.filePaths == null) {
            final var errorMessage =
                    "Keine Dateipfade angelegt für Deskriptor: " + fileDescriptor;
            throw new IllegalStateException(errorMessage);
        }
    }

    /**
     * Creates the filenames.
     *
     * @param pathExtension the path extension
     *
     * @throws IllegalStateException the illegal state exception
     */
    public
    void createFileReferences (final String pathExtension)
    throws IllegalStateException, InterruptedException {

        if (fileDescriptor.isIdRequired()) {

            this.createFileLockReference(pathExtension);
            this.createWorkingFileReferences(pathExtension);
        } else {
            final var errorMessage = "Dateipfade konnten nicht angelegt werden. " +
                                     "Dateideskriptor: " + fileDescriptor +
                                     " wird keine Pfaderweiterung benötigt.";
            throw new IllegalStateException(errorMessage);
        }
    }

    private
    void createFileLockReference (final String pathExtension)
    throws InterruptedException {
        String directory = PersistentDataLocationCreator.createDirectoryPath(
                PersistentDataLocationCreator.DataOwnershipDescriptor.CLIENT,
                pathExtension);
        createSingleFileReference(directory, this.fileDescriptor.getLockFilename());
    }

    private
    void createWorkingFileReferences (final String pathExtension)
    throws InterruptedException {

        if (pathExtension != null && !pathExtension.isEmpty()) {
            String[] directories = PersistentDataLocationCreator.createDirectoryPaths(
                    PersistentDataLocationCreator.DataOwnershipDescriptor.CLIENT,
                    pathExtension);

            if (directories != null) {
                createMultipleFileReferences(directories,
                                             this.fileDescriptor.getDataFilename());
            } else {
                throw new IllegalArgumentException(
                        "Keine gueltigen Verzeichnispfade uebergeben.");
            }
        } else {
            final var errorMessage = "Dateipfade konnten nicht angelegt werden. " +
                                     "Dateideskriptor: " + fileDescriptor +
                                     " wird eine Pfaderweiterung benötigt.";
            throw new IllegalStateException(errorMessage);
        }
    }

    /**
     * Tries to read from file and backup file. Returns first content found. If all
     * this.fileChannels are empty, null is returned.
     *
     * @return object
     */
    public
    Object readData () {

        if (this.globalLock == null) {
            throw new IllegalStateException(NO_FILE_ACCESS_ACQUIRED);
        }

        Object readObject = null;
        final var lastChangedFile = this.getLatestChangedFile();

        try {
            String readJsonString = Files.readString(lastChangedFile);
            readObject = mapper.readValue(readJsonString, dataFormat);
        } catch (JsonParseException | JsonMappingException je) {
            LOGGER.info("Gelesene Daten nicht instanziierbar: {}\n{}: {}",
                        lastChangedFile, je.getClass().getSimpleName(),
                        je.getMessage());
        } catch (final FileNotFoundException ex) {
            Thread.currentThread().interrupt();
            LOGGER.info("Datei nicht gefunden: {}\n{}", lastChangedFile,
                        asList(ex.getStackTrace()));
        } catch (final IOException ex) {
            Thread.currentThread().interrupt();
            LOGGER.info("Lesen aus Datei fehlgeschlagen: {}\n{}",
                        lastChangedFile, asList(ex.getStackTrace()));
        }
        return readObject;
    }

    private
    Path getLatestChangedFile () {
        Path upToDateFile = null;
        Instant lastChangedTime = Instant.MIN;

        for (final var currentFile : this.filePaths) {
            Instant modificationTime = Instant.ofEpochMilli(
                    currentFile.toFile().lastModified());

            if (modificationTime.isAfter(lastChangedTime)) {
                lastChangedTime = modificationTime;
                upToDateFile = currentFile;
            }
        }
        return upToDateFile;
    }

    public
    void removeFileReferences () {
        this.releaseAccess();
        this.filePaths = null;
        this.lockFilePath = null;
    }

    /**
     * Release accessLimiter.
     */
    public
    void releaseAccess () {
        try {
            this.localLock.readLock().unlock();
        }catch(IllegalMonitorStateException imse){
            LOGGER.trace("ReadLock nicht lokal gehalten.\n{}", asList(imse.getStackTrace()));
        }
        try {
            this.localLock.writeLock().unlock();
        }catch(IllegalMonitorStateException imse){
            LOGGER.trace("WriteLock nicht lokal gehalten.\n{}", asList(imse.getStackTrace()));
        }

        if (this.globalLock != null) {
            try {
                final var channel = this.globalLock.channel();
                this.globalLock.release();
                channel.close();
            } catch (IOException e) {
                LOGGER.info("Dateischloss konnte nicht geöffnet werden. {}\n{}",
                            e.getClass().getSimpleName(), asList(e.getStackTrace()));
            }
        }
        this.globalLock = null;
    }

    /**
     * Overwrites file and backup file content with argument, if argument is not
     * null.
     *
     * @param toWrite the to write
     *
     * @return true, if successful
     *
     * @throws IllegalStateException if no data file pathNames are set
     */
    public
    boolean writeData (final Object toWrite) {
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
                    LOGGER.info("Nach {} konnte nicht geschrieben werden." + "\n{}",
                                currentPath, asList(e.getStackTrace()));
                    return !dataWritten;
                }
            }
            return dataWritten;
        }
        return !dataWritten;
    }

    private
    String generateJsonFromObject (final Object toWrite) {
        //TODO das hier sollte in einem eigenen Objekt (statische Methode) erstellt werden
        // TODO am besten komplett verschlüsselt
        try {
            return this.mapper.writerFor(this.dataFormat).writeValueAsString(toWrite);
        } catch (JsonProcessingException je) {
            LOGGER.info("{}: {}", je.getClass().getSimpleName(), je.getMessage());
        }

        return null;
    }
}
