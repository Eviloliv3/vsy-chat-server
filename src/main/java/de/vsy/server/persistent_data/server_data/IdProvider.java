/*
 *
 */
package de.vsy.server.persistent_data.server_data;

import static com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance;
import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_CLIENT_ID;

import com.fasterxml.jackson.databind.JavaType;
import de.vsy.server.persistent_data.PersistenceDAO;
import de.vsy.server.persistent_data.PersistentDataFileCreator.DataFileDescriptor;
import de.vsy.server.persistent_data.server_data.temporal.IdProviderPool;
import de.vsy.server.persistent_data.server_data.temporal.IdType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Creates new unique client ids by reading the last unused id from a file, incrementing and
 * rewriting the incremented id. The read id is returned to the calling object.
 */
public class IdProvider implements ServerDataAccess {

  private static final Logger LOGGER = LogManager.getLogger();
  private final PersistenceDAO dataProvider;

  public IdProvider() {

    this.dataProvider = new PersistenceDAO(DataFileDescriptor.ID_MAP, getDataFormat());
  }

  /**
   * Returns the dataManagement format.
   *
   * @return the dataManagement format
   */
  public static JavaType getDataFormat() {
    final var factory = defaultInstance();
    return factory.constructType(IdProviderPool.class);
  }

  @Override
  public void createFileAccess() throws InterruptedException {
    this.dataProvider.createFileReferences();
  }

  /**
   * Returns the new communicator id.
   *
   * @return the new communicator id
   */
  public int getNewId(IdType forType) {
    IdProviderPool idPool;
    int newId;

    if (!this.dataProvider.acquireAccess(true)) {
      LOGGER.error("No exclusive write access.");
      return STANDARD_CLIENT_ID;
    }
    idPool = readIdProvider();
    newId = idPool.getNextId(forType);
    this.dataProvider.writeData(idPool);
    this.dataProvider.releaseAccess(true);
    return newId;
  }

  public void returnId(IdType type, int idToReturn) {
    IdProviderPool idPool;

    if (!this.dataProvider.acquireAccess(true)) {
      LOGGER.error("No exclusive write access.");
    }
    idPool = readIdProvider();
    idPool.returnUnusedId(type, idToReturn);
    this.dataProvider.writeData(idPool);
    this.dataProvider.releaseAccess(true);
  }

  /**
   * Read id map.
   *
   * @return the map
   */
  private IdProviderPool readIdProvider() {
    IdProviderPool noPool = null;
    Object fromFile;
    if (!this.dataProvider.acquireAccess(false)) {
      LOGGER.error("No shared read access.");
      return null;
    }
    fromFile = this.dataProvider.readData();
    this.dataProvider.releaseAccess(false);

    if (fromFile instanceof final IdProviderPool idPool) {
      return idPool;
    }
    return null;
  }

  @Override
  public void removeFileAccess() {
    this.dataProvider.removeFileReferences();
  }
}
