package de.vsy.chat.server.testing_grounds;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestFileChannel {

	@Test
	public void checkUniqueness() throws IOException {
		RandomAccessFile file1 = null, file2 = null;
		FileChannel channel1 = null, channel2 = null;
		FileLock lock1 = null, lock2 = null;
		try {
			try {
				file1 = new RandomAccessFile(
						"/home/fredward/Dokumente/VSY_Chat_Server_Daten/data/clientTransactionsUTF_8.json", "rw");
				channel1 = file1.getChannel();
				lock1 = channel1.lock(0L, Long.MAX_VALUE, true);
				Assertions.assertEquals(channel1, lock1.channel());
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
			/*
			 * try { file2 = new RandomAccessFile(
			 * "/home/fredward/Dokumente/VSY_Chat_Server_Daten/data/clientTransactionsUTF_8.json"
			 * ,"rw"); channel2 = file2.getChannel(); lock2 = channel2.lock(0L,
			 * Long.MAX_VALUE, true); Assertions.assertTrue(lock2.isValid());
			 * channel2.close(); file2.close(); }catch(IOException ioe){
			 * ioe.printStackTrace(); }
			 */
		} finally {
			channel1.close();
			file1.close();
		}
	}
}
