/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.vsy.server.persistent_data;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.apache.logging.log4j.Logger;

/**
 * Simple tool for creating preset file types. Assuming the passed directories
 * have already been created. fredward
 */
public class PersistentDataFileCreator {

	/** The Enum DataFileDescriptor. */
	public enum DataFileDescriptor implements RequirementProvider {
		/* The active clients */
		ACTIVE_CLIENTS(false, "activeClientsUTF_8.json", "activeClients.lock"),
		CLIENT_TRANSACTION(false, "clientTransactionsUTF_8.json", "clientTransactions.lock"),
		COMMUNICATORS(false, "communicatorsUTF_8.json", "communicatorData.lock"),
		CONTACT_LIST(true, "contactListUTF_8.json", "contactList.lock"), ID_MAP(false, "IdMapUTF_8.json", "IdMap.lock"),
		MESSAGE_HISTORY(true, "messagesUTF_8.json", "messageHistory.lock"),
		PENDING_PACKETS(true, "pendingPacketsUTF_8.json", "pendingPacket.lock"),
		REGISTERED_CLIENTS(false, "registeredClientsUTF_8.json", "registeredClients.lock");

		private final boolean idRequirement;
		private final String dataFilename;
		private final String lockFilename;

		/**
		 * Instantiates a new dataManagement file descriptor.
		 *
		 * @param idRequired the id required
		 */
		DataFileDescriptor(final boolean idRequired, final String dataFilename, final String lockFilename) {
			this.idRequirement = idRequired;
			this.dataFilename = dataFilename;
			this.lockFilename = lockFilename;
		}

		@Override
		public boolean isIdRequired() {
			return this.idRequirement;
		}

		public String getDataFilename() {
			return this.dataFilename;
		}

		public String getLockFilename() {
			return this.lockFilename;
		}
	}

	/**
	 * Creates the or get file.
	 *
	 * @param directoryNames the directory names
	 * @param filename       the filename
	 * @param LOGGER         the LOGGER
	 *
	 * @return the string[]
	 */
	public static Path[] createAndGetFilePaths(final String[] directoryNames, final String filename,
			final Logger LOGGER) {
		Path[] files;
		final int fileCount = directoryNames.length;
		files = new Path[fileCount];

		for (var i = 0; i < fileCount; i++) {
			final Path currentReference = createAndGetFilePath(directoryNames[i], filename, LOGGER);

			if (currentReference == null) {
				files = null;
				break;
			} else {
				files[i] = currentReference;
			}
		}
		return files;
	}

	public static Path createAndGetFilePath(final String directoryName, final String filename, final Logger LOGGER) {
		Path fileReference = null;
		final var directory = new File(directoryName);

		try {

			if (directory.isDirectory()) {
				fileReference = Path.of(directoryName, filename);
				final var file = fileReference.toFile();

				if (!file.isFile()) {
					var newFileIsCreated = file.createNewFile();

					if (!newFileIsCreated) {
						LOGGER.error("Datei {} sollte neu erstellt " + "werden, existierte aber schon.", fileReference);
					}
				} else {
					LOGGER.trace("Datei existiert bereits: {}", fileReference);
				}
			} else {
				LOGGER.error("Pfad ist kein gueltiges Verzeichnis: {}", directory);
			}
		} catch (final SecurityException se) {
			Thread.currentThread().interrupt();
			LOGGER.error("Datei (Pfad: {}: {}) durfte nicht erstellt werden. (SecurityException)", directoryName,
					filename);
			fileReference = null;
		} catch (final IOException ex) {
			Thread.currentThread().interrupt();
			LOGGER.error("Datei (Pfad: {}: {}) konnte nicht erstellt werden. (IOException)\n{}", directoryName,
					filename, asList(ex.getStackTrace()));
			fileReference = null;
		}
		return fileReference;
	}
}
