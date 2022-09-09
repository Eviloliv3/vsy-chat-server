/*
 *
 */
package de.vsy.server.persistent_data.client_data;

import com.fasterxml.jackson.databind.JavaType;
import de.vsy.server.persistent_data.PersistenceDAO;
import de.vsy.server.persistent_data.PersistentDataFileCreator.DataFileDescriptor;
import de.vsy.shared_transmission.shared_transmission.packet.content.relation.EligibleContactEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

import static com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance;
import static java.lang.String.valueOf;

/** Grants accessLimiter to the file containing a client's contactSet. */
public
class ContactListDAO implements ClientDataAccess {

    private static final Logger LOGGER = LogManager.getLogger();
    private final PersistenceDAO dataProvider;

    /** Instantiates a new contactSet modifier. */
    public
    ContactListDAO () {

        this.dataProvider = new PersistenceDAO(DataFileDescriptor.CONTACT_LIST,
                                               getDataFormat());
    }

    /**
     * Gets the dataManagement format.
     *
     * @return the dataManagement format
     */
    public static
    JavaType getDataFormat () {
        final var factory = defaultInstance();
        final var contactKeys = factory.constructType(EligibleContactEntity.class);
        final JavaType contactValues = factory.constructCollectionType(HashSet.class,
                                                                       Integer.class);
        return factory.constructMapType(EnumMap.class, contactKeys, contactValues);
    }

    /**
     * Adds the contact to list.
     *
     * @param contactType the contact type
     * @param clientId the client id
     *
     * @return true, if successful
     */
    public
    boolean addContact (final EligibleContactEntity contactType,
                        final int clientId) {
        var contactAdded = false;
        Map<EligibleContactEntity, Set<Integer>> contactMap;
        Set<Integer> contactSet;

        final var lockAlreadyAcquired = this.dataProvider.checkForActiveLock();

        if (!lockAlreadyAcquired) {
            if(this.dataProvider.acquireAccess(true))
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

        if (!lockAlreadyAcquired) {
            this.dataProvider.releaseAccess();
        }

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
    public
    Map<EligibleContactEntity, Set<Integer>> readContactMap () {
        var readMap = new EnumMap<EligibleContactEntity, Set<Integer>>(
                EligibleContactEntity.class);
        Object fromFile;
        final var lockAlreadyAcquired = this.dataProvider.checkForActiveLock();

        if (!lockAlreadyAcquired) {
            this.dataProvider.acquireAccess(false);
        }
        fromFile = this.dataProvider.readData();

        if (!lockAlreadyAcquired) {
            this.dataProvider.releaseAccess();
        }

        if (fromFile instanceof EnumMap) {

            try {
                readMap = (EnumMap<EligibleContactEntity, Set<Integer>>) fromFile;
            } catch (final ClassCastException cc) {
                LOGGER.info(
                        "ClassCastException beim Lesen der Freundesliste. Die Sete wird leer ausgegeben.");
            }
        }
        return readMap;
    }

    public
    boolean checkContact (final EligibleContactEntity contactType,
                          final int contactId) {
        boolean isContact = false;
        final var lockAlreadyAcquired = this.dataProvider.checkForActiveLock();

        if (!lockAlreadyAcquired) {
            this.dataProvider.acquireAccess(false);
        }
        final var contacts = this.readContacts(contactType);
        isContact = contacts.contains(contactId);

        if (!lockAlreadyAcquired) {
            this.dataProvider.releaseAccess();
        }
        return isContact;
    }

    /**
     * Read contacts.
     *
     * @param contactType the contact type
     *
     * @return the array list
     */
    public
    Set<Integer> readContacts (final EligibleContactEntity contactType) {
        Map<EligibleContactEntity, Set<Integer>> readMap;
        Set<Integer> readContacts;
        final var lockAlreadyAcquired = this.dataProvider.checkForActiveLock();

        if (!lockAlreadyAcquired) {
            this.dataProvider.acquireAccess(false);
        }
        readMap = readContactMap();
        readContacts = readMap.get(contactType);

        if (!lockAlreadyAcquired) {
            this.dataProvider.releaseAccess();
        }

        if (readContacts == null) {
            readContacts = new HashSet<>();
        }
        return readContacts;
    }

    /**
     * Check acquaintance.
     *
     * @param contactType the contact type
     * @param contactId the contact id
     *
     * @return true, if successful
     */
    public
    boolean checkAcquaintance (final EligibleContactEntity contactType,
                               final int contactId) {
        var acquaintanceState = false;
        Set<Integer> contactSet;

        final var lockAlreadyAcquired = this.dataProvider.checkForActiveLock();

        if (!lockAlreadyAcquired) {
            this.dataProvider.acquireAccess(false);
        }
        contactSet = readContacts(contactType);

        if (!lockAlreadyAcquired) {
            this.dataProvider.releaseAccess();
        }

        if (contactSet != null) {
            acquaintanceState = contactSet.contains(contactId);
        }

        return acquaintanceState;
    }

    /**
     * Check acquaintance.
     *
     * @param contactId the contact id
     *
     * @return true, if successful
     */
    public
    boolean checkAcquaintance (final int contactId) {
        var acquaintanceState = false;
        Map<EligibleContactEntity, Set<Integer>> contactMap;
        Set<Integer> contactSet;

        final var lockAlreadyAcquired = this.dataProvider.checkForActiveLock();

        if (!lockAlreadyAcquired) {
            this.dataProvider.acquireAccess(false);
        }
        contactMap = readContactMap();

        if (!lockAlreadyAcquired) {
            this.dataProvider.releaseAccess();
        }

        for (final EligibleContactEntity contactType : EligibleContactEntity.values()) {
            contactSet = contactMap.getOrDefault(contactType,
                                                 Collections.emptySet());

            if (contactSet.contains(contactId)) {
                acquaintanceState = true;
                break;
            }
        }

        return acquaintanceState;
    }

    @Override
    public
    void createFileAccess (final int clientId)
    throws InterruptedException {
        this.dataProvider.createFileReferences(valueOf(clientId));
    }

    /**
     * Removes the contact from list.
     *
     * @param contactType the contact type
     * @param clientId the client id
     *
     * @return true, if successful
     */
    public
    boolean removeContactFromSet (final EligibleContactEntity contactType,
                                  final int clientId) {
        var contactRemoved = false;
        Map<EligibleContactEntity, Set<Integer>> contactMap;
        Set<Integer> contactSet;

        final var lockAlreadyAcquired = this.dataProvider.checkForActiveLock();

        if (!lockAlreadyAcquired) {
            if(this.dataProvider.acquireAccess(true))
                return false;
        }
        contactMap = readContactMap();
        contactSet = contactMap.get(contactType);

        if (contactSet != null) {
            contactRemoved = contactSet.remove(clientId);
            contactMap.put(contactType, contactSet);
            contactRemoved &= this.dataProvider.writeData(contactMap);
        }

        if (!lockAlreadyAcquired) {
            this.dataProvider.releaseAccess();
        }

        if (contactRemoved) {
            LOGGER.info("Contact removed.");
        } else {
            LOGGER.info("Contact not removed.");
        }
        return contactRemoved;
    }

    @Override
    public
    void removeFileAccess () {
        this.dataProvider.removeFileReferences();
    }
}
