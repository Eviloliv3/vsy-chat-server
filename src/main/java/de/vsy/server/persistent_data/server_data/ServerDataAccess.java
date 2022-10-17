package de.vsy.server.persistent_data.server_data;

import de.vsy.server.persistent_data.PersistentDataAccess;

/** Aktiviert Dateizugriff auf Server eigene Daten zur Laufzeit. */
public interface ServerDataAccess extends PersistentDataAccess {

	/**
	 * Creates the file accessLimiter.
	 *
	 * @throws IllegalStateException    the illegal state exception
	 * @throws IllegalArgumentException the illegal argument exception
	 * @throws InterruptedException     the interrupted exception
	 */
	void createFileAccess() throws IllegalStateException, IllegalArgumentException, InterruptedException;
}
