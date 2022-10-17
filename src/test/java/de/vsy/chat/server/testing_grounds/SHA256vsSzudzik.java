package de.vsy.chat.server.testing_grounds;

import static java.lang.String.valueOf;
import static java.lang.System.nanoTime;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.security.MessageDigest.getInstance;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

class SHA256vsSzudzik {

	static final Logger LOGGER = LogManager.getLogger();
	static final int id1 = 18000;
	static final int id2 = 16052;

	@Test
	void checkCollision() throws NoSuchAlgorithmException {
		List<String> hash = new ArrayList<>();
		byte[] digested;
		BigInteger msgInteger;

		var md = getInstance("SHA-256");

		// Einfaches Update
		md.update(valueOf(id1).getBytes(UTF_8));
		md.update(valueOf(id2).getBytes(UTF_8));

		digested = md.digest();
		msgInteger = new BigInteger(1, digested);
		hash.add(msgInteger.toString(16).toUpperCase());

		// ZwischenDigest
		md.update(valueOf(id1).getBytes(UTF_8));
		md.update(valueOf(id2).getBytes(UTF_8));

		digested = md.digest();
		msgInteger = new BigInteger(1, digested);
		hash.add(msgInteger.toString(16).toUpperCase());

		for (var currentHash : hash) {
		}
	}

	@Test
	void timingChange() throws NoSuchAlgorithmException {
		List<String> hash = new ArrayList<>();
		var testIds = new int[] { 15001, 17052, 18354 };
		byte[] digested;
		BigInteger msgInteger;
		long allstarttime, starttime, allendtime, endtime, overalltime = 0;
		MessageDigest md;

		allstarttime = nanoTime();
		for (var id : testIds) {
			starttime = nanoTime();
			md = getInstance("SHA-256");

			// Einfaches Update
			md.update(valueOf(id1).getBytes(UTF_8));
			md.update(valueOf(id).getBytes(UTF_8));

			digested = md.digest();
			msgInteger = new BigInteger(1, digested);
			hash.add(msgInteger.toString(16).toUpperCase());
			endtime = nanoTime();
			overalltime += endtime - starttime;
		}
		allendtime = nanoTime();
		overalltime += allendtime - allstarttime;
		LOGGER.info("SHA komplett neu braucht: {} Sekunden", MICROSECONDS.convert(overalltime, NANOSECONDS));

		allstarttime = nanoTime();
		md = getInstance("SHA-256");
		md.update(valueOf(id1).getBytes(UTF_8));
		digested = md.digest();

		for (var id : testIds) {
			starttime = nanoTime();

			md = getInstance("SHA-256");
			md.update(digested);
			md.update(valueOf(id).getBytes(UTF_8));

			digested = md.digest();
			msgInteger = new BigInteger(1, digested);
			hash.add(msgInteger.toString(16).toUpperCase());
			endtime = nanoTime();
			overalltime += endtime - starttime;
		}
		allendtime = nanoTime();
		LOGGER.info(MILLISECONDS.convert(allendtime - allstarttime, NANOSECONDS));
	}

	@Test
	void checkSzudzikCollision() {
		int ergebnis;
		var a = id1;
		var b = id2;

		if (a >= b) {
			ergebnis = a * a + a + b;
		} else {
			ergebnis = a + b * b;
		}
		LOGGER.info("Richtig herum {}", ergebnis);
		a = id2;
		b = id1;
		if (a >= b) {
			ergebnis = a * a + a + b;
		} else {
			ergebnis = a + b * b;
		}
		LOGGER.info("Falsch herum {}", ergebnis);
	}

	@Test
	void testSHA256() throws NoSuchAlgorithmException {
		String hash;
		byte[] digested;
		BigInteger msgInteger;
		long starttime, endtime, ergebnis;
		var md = getInstance("SHA-256");

		starttime = nanoTime();
		md.update(valueOf(id1).getBytes(UTF_8));
		digested = md.digest();
		md.update(digested);
		md.update(valueOf(id2).getBytes(UTF_8));
		digested = md.digest();
		msgInteger = new BigInteger(1, digested);
		hash = msgInteger.toString(16).toUpperCase();

		LOGGER.info(hash);

		endtime = nanoTime();

		LOGGER.info("SHA braucht: {}", MICROSECONDS.convert((endtime - starttime), NANOSECONDS) + " Sekunden");

		starttime = nanoTime();
		if (id1 >= id2) {
			ergebnis = id1 * id1 + id1 + id2;
		} else {
			ergebnis = id1 + id2 * id2;
		}
		LOGGER.info(ergebnis);
		endtime = nanoTime();
		LOGGER.info("Gesamtzeit: {}", TimeUnit.NANOSECONDS.convert((endtime - starttime), TimeUnit.MILLISECONDS));
	}
}
