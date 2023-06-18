package de.vsy.server.persistent_data;

/**
 * The Enum DataFileDescriptor.
 */
public enum DataFileDescriptor {
    /* The active clients */
    ACTIVE_CLIENTS(false, "activeClientsUTF_8.json"),
    CLIENT_TRANSACTION(false, "clientTransactionsUTF_8.json"),
    COMMUNICATORS(false, "communicatorsUTF_8.json"),
    CONTACT_LIST(true, "contactListUTF_8.json"), ID_MAP(false,
            "IdMapUTF_8.json"),
    MESSAGE_HISTORY(true, "messagesUTF_8.json"),
    PENDING_PACKETS(true, "pendingPacketsUTF_8.json"),
    REGISTERED_CLIENTS(false, "registeredClientsUTF_8.json");

    private final boolean extensionRequired;
    private final String dataFilename;

    /**
     * Instantiates a new dataManagement file descriptor.
     *
     * @param extensionRequired the extensionFlag
     * @param dataFilename      the filename
     */
    DataFileDescriptor(final boolean extensionRequired, final String dataFilename) {
        this.extensionRequired = extensionRequired;
        this.dataFilename = dataFilename;
    }

    public boolean pathExtensionRequired() {
        return this.extensionRequired;
    }

    public String getDataFilename() {
        return this.dataFilename;
    }
}
