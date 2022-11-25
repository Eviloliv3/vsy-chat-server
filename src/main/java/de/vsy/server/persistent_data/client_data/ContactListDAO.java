/*
 *
 */
package de.vsy.server.persistent_data.client_data;

import static com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance;
import static java.lang.String.valueOf;

import com.fasterxml.jackson.databind.JavaType;
import de.vsy.server.persistent_data.PersistenceDAO;
import de.vsy.server.persistent_data.PersistentDataFileCreator.DataFileDescriptor;
import de.vsy.shared_transmission.packet.content.relation.EligibleContactEntity;
import de.vsy.shared_transmission.packet.content.relation.EligibleContactEntity;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Grants accessLimiter to the file containing a client's contactSet.
 */
public class ContactListDAO implements ClientDataAccess {

  private static final Logger LOGGER = LogManager.getLogger();
  private final PersistenceDAO dataProvider;

  /**
   * Instantiates a new contactSet modifier.
   */
  public ContactListDAO() {

    this.dataProvider = new PersistenceDAO(DataFileDescriptor.CONTACT_LIST, getDataFormat());
  }

  /**
   * Gets the dataManagement format.
   *
   * @return the dataManagement format
   */
  public static JavaType getDataFormat() {
    final var factory = defaultInstance();
    final var contactKeys = factory.constructType(EligibleContactEntity.class);
    final JavaType contactValues = factory.constructCollectionType(HashSet.class, Integer.class);
    return factory.constructMapType(EnumMap.class, contactKeys, contactValues);
  }

  /**
   * Adds the contact to list.
   *
   * @param contactType the contact type
   * @param clientId    the client id
   * @return true, if successful
   */
  public boolean addContact(final EligibleContactEntity contactType, final int clientId) {
    var contactAdded = false;
    Map<EligibleContactEntity, Set<Integer>> contactMap;
    Set<Integer> contactSet;

    if (!this.dataProvider.acquireAccess(true)) {
      LOGGER.error("No exclusive write access.");
      return false;
    }
    contactMap = readContactMap();
    contactSet = contactMap.get(contactType);

    if (contactSet == null) {
      contactSet = new HashSet<>();
    }

    if (contactSet.add(clientId)) {
      contactMap.put(contactType, contactSet);
      contactAdded = this.dataProvider.writeData(contactMap);
    }
    this.dataProvider.releaseAccess(true);

    if (contactAdded) {
      LOGGER.info("Contact added.");
    } else {
      LOGGER.info("Contact not added.");
    }
    return contactAdded;
  }

  /**
   * Read contactSet.
   *
   * @return the array list
   */
  @SuppressWarnings("unchecked")
  public Map<EligibleContactEntity, Set<Integer>> readContactMap() {
    var readMap = new EnumMap<EligibleContactEntity, Set<Integer>>(EligibleContactEntity.class);
    Object fromFile;

    if (!this.dataProvider.acquireAccess(false)) {
      LOGGER.error("No shared read access.");
      return readMap;
    }
    fromFile = this.dataProvider.readData();
    this.dataProvider.releaseAccess(false);

    if (fromFile instanceof EnumMap) {

      try {
        readMap = (EnumMap<EligibleContactEntity, Set<Integer>>) fromFile;
      } catch (final ClassCastException cc) {
        LOGGER.info(
            "{} occurred while reading the contact list. Empty map will be returned.",
            cc.getClass().getSimpleName());
      }
    }
    return readMap;
  }

  public boolean checkContact(final EligibleContactEntity contactType, final int contactId) {
    final boolean isContact;
    final Set<Integer> contacts;

    if (!this.dataProvider.acquireAccess(false)) {
      LOGGER.error("No shared read access.");
      return false;
    }
    contacts = this.readContacts(contactType);
    this.dataProvider.releaseAccess(false);
    isContact = contacts.contains(contactId);
    return isContact;
  }

  /**
   * Read contacts.
   *
   * @param contactType the contact type
   * @return the array list
   */
  public Set<Integer> readContacts(final EligibleContactEntity contactType) {
    Map<EligibleContactEntity, Set<Integer>> readMap;
    Set<Integer> readContacts;

    if (!this.dataProvider.acquireAccess(false)) {
      LOGGER.error("No shared read access.");
      return new HashSet<>();
    }
    readMap = readContactMap();
    this.dataProvider.releaseAccess(false);
    readContacts = readMap.get(contactType);

    if (readContacts == null) {
      readContacts = new HashSet<>();
    }
    return readContacts;
  }

  /**
   * Check acquaintance.
   *
   * @param contactType the contact type
   * @param contactId   the contact id
   * @return true, if successful
   */
  public boolean checkAcquaintance(final EligibleContactEntity contactType, final int contactId) {
    var acquaintanceState = false;
    Set<Integer> contactSet;

    if (!this.dataProvider.acquireAccess(false)) {
      LOGGER.error("No shared read access.");
      return false;
    }
    contactSet = readContacts(contactType);
    this.dataProvider.releaseAccess(false);

    if (contactSet != null) {
      acquaintanceState = contactSet.contains(contactId);
    }

    return acquaintanceState;
  }

  /**
   * Check acquaintance.
   *
   * @param contactId the contact id
   * @return true, if successful
   */
  public boolean checkAcquaintance(final int contactId) {
    var acquaintanceState = false;
    Map<EligibleContactEntity, Set<Integer>> contactMap;
    Set<Integer> contactSet;

    if (!this.dataProvider.acquireAccess(false)) {
      LOGGER.error("No shared read access.");
      return false;
    }
    contactMap = readContactMap();
    this.dataProvider.releaseAccess(false);

    for (final EligibleContactEntity contactType : EligibleContactEntity.values()) {
      contactSet = contactMap.getOrDefault(contactType, Collections.emptySet());

      if (contactSet.contains(contactId)) {
        acquaintanceState = true;
        break;
      }
    }

    return acquaintanceState;
  }

  @Override
  public void createFileAccess(final int clientId) throws InterruptedException {
    this.dataProvider.createFileReferences(valueOf(clientId));
  }

  /**
   * Removes the contact from list.
   *
   * @param contactType the contact type
   * @param clientId    the client id
   * @return true, if successful
   */
  public boolean removeContactFromSet(final EligibleContactEntity contactType, final int clientId) {
    var contactRemoved = false;
    Map<EligibleContactEntity, Set<Integer>> contactMap;
    Set<Integer> contactSet;

    if (!this.dataProvider.acquireAccess(true)) {
      LOGGER.error("No exclusive write access.");
      return false;
    }
    contactMap = readContactMap();
    contactSet = contactMap.get(contactType);

    if (contactSet != null) {
      contactRemoved = contactSet.remove(clientId);
      contactMap.put(contactType, contactSet);
      contactRemoved &= this.dataProvider.writeData(contactMap);
    }

    this.dataProvider.releaseAccess(true);

    if (contactRemoved) {
      LOGGER.info("Contact removed.");
    } else {
      LOGGER.info("Contact not removed.");
    }
    return contactRemoved;
  }

  @Override
  public void removeFileAccess() {
    this.dataProvider.removeFileReferences();
  }
}
