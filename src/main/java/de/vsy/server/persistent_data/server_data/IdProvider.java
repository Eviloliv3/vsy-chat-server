/*
 *
 */
package de.vsy.server.persistent_data.server_data;

import static com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance;
import static de.vsy.shared_utility.standard_value.StandardIdProvider.STANDARD_CLIENT_ID;

import com.fasterxml.jackson.databind.JavaType;
import de.vsy.server.persistent_data.PersistenceDAO;
import de.vsy.server.persistent_data.PersistentDataFileCreator.DataFileDescriptor;
import de.vsy.shared_module.shared_module.data_element_validation.IdCheck;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Creates new uniqü client ids by reading the last unused id from a file, incrementing and
 * rewriting the incremented id. The read id is returned to the calling object.
 */
public class IdProvider implements ServerDataAccess {

  private static final Logger LOGGER = LogManager.getLogger();
  private final PersistenceDAO dataProvider;

  public IdProvider() {

    this.dataProvider = new PersistenceDAO(DataFileDescriptor.ID_MAP, getDataFormat());
  }

  /**
   * Gets the dataManagement format.
   *
   * @return the dataManagement format
   */
  public static JavaType getDataFormat() {
    final var factory = defaultInstance();
    return factory.constructMapType(HashMap.class, String.class, Integer.class);
  }

  @Override
  public void createFileAccess() throws InterruptedException {
    this.dataProvider.createFileReferences();
  }

  /**
   * Gets the new communicator id.
   *
   * @return the new communicator id
   */
  public int getNewId() {
    Map<String, Integer> idMap;
    int newId;

        if (!this.dataProvider.acquireAccess(true)) {LOGGER.error("Kein exklusiver Schreibzugriff moeglich.");
      return STANDARD_CLIENT_ID;
    }
    idMap = readIdMap();
    newId = idMap.get("client");

    if (IdCheck.checkData(newId).isPresent()) {
      newId = 15000;
      LOGGER.warn("IDs zurückgesetzt.");
    }
    idMap.put("client", newId + 1);
    this.dataProvider.writeData(idMap);

    this.dataProvider.releaseAccess(true);

    return newId;
  }

  /**
   * Read id map.
   *
   * @return the map
   */
  @SuppressWarnings("unchecked")
  Map<String, Integer> readIdMap() {
    Object fromFile;
    var readMap = new HashMap<String, Integer>();
        if (!this.dataProvider.acquireAccess(false)) {LOGGER.error("Kein Lesezugriff moeglich.");
      return readMap;
    }
    fromFile = this.dataProvider.readData();
    this.dataProvider.releaseAccess(false);

    if (fromFile instanceof HashMap) {

      try {
        readMap = (HashMap<String, Integer>) fromFile;
      } catch (final ClassCastException cc) {
        LOGGER.info("ClassCastException beim Lesen der Id-Map. Die Map wird leer ausgegeben.");
      }
    }
    return readMap;
  }

  @Override
  public void removeFileAccess() {
    this.dataProvider.removeFileReferences();
  }
}
