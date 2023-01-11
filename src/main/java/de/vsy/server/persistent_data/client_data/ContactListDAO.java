/*
 *
 */
package de.vsy.server.persistent_data.client_data;

import com.fasterxml.jackson.databind.JavaType;
import de.vsy.server.persistent_data.SynchronousFileManipulator;
import de.vsy.server.persistent_data.DataFileDescriptor;
import de.vsy.shared_transmission.packet.content.relation.EligibleContactEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

import static com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance;
import static java.lang.String.valueOf;

/**
 * Grants accessLimiter to the file containing a client's contactSet.
 */
public class ContactListDAO extends ClientDAO {

    /**
     * Instantiates a new contactSet modifier.
     */
    public ContactListDAO() {
        super(DataFileDescriptor.CONTACT_LIST, getDataFormat());
    }

    /**
     * Returns the dataManagement format.
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

        if (!super.dataProvider.acquireAccess(false)) {
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
            contactAdded = super.dataProvider.writeData(contactMap);
        }
        super.dataProvider.releaseAccess(false);

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

        if (!super.dataProvider.acquireAccess(true)) {
            LOGGER.error("No shared read access.");
            return readMap;
        }
        fromFile = super.dataProvider.readData();
        super.dataProvider.releaseAccess(true);

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

        if (!super.dataProvider.acquireAccess(true)) {
            LOGGER.error("No shared read access.");
            return false;
        }
        contacts = this.readContacts(contactType);
        super.dataProvider.releaseAccess(true);
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

        if (!super.dataProvider.acquireAccess(true)) {
            LOGGER.error("No shared read access.");
            return new HashSet<>();
        }
        readMap = readContactMap();
        super.dataProvider.releaseAccess(true);
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

        if (!super.dataProvider.acquireAccess(true)) {
            LOGGER.error("No shared read access.");
            return false;
        }
        contactSet = readContacts(contactType);
        super.dataProvider.releaseAccess(true);

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

        if (!super.dataProvider.acquireAccess(true)) {
            LOGGER.error("No shared read access.");
            return false;
        }
        contactMap = readContactMap();
        super.dataProvider.releaseAccess(true);

        for (final EligibleContactEntity contactType : EligibleContactEntity.values()) {
            contactSet = contactMap.getOrDefault(contactType, Collections.emptySet());

            if (contactSet.contains(contactId)) {
                acquaintanceState = true;
                break;
            }
        }

        return acquaintanceState;
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

        if (!super.dataProvider.acquireAccess(false)) {
            LOGGER.error("No exclusive write access.");
            return false;
        }
        contactMap = readContactMap();
        contactSet = contactMap.get(contactType);

        if (contactSet != null) {
            contactRemoved = contactSet.remove(clientId);
            contactMap.put(contactType, contactSet);
            contactRemoved &= super.dataProvider.writeData(contactMap);
        }

        super.dataProvider.releaseAccess(false);

        if (contactRemoved) {
            LOGGER.info("Contact removed.");
        } else {
            LOGGER.info("Contact not removed.");
        }
        return contactRemoved;
    }
}
