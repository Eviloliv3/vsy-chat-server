package de.vsy.chat.server.testing_grounds;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TestLinuxShutdownGrace {

  public static void main(String[] args) throws IOException {
    ServerSocket ss = new ServerSocket(8080);
    ExecutorService es = Executors.newSingleThreadExecutor();
    Thread testThread = new Thread() {
      @Override
      public void run() {
        System.out.println("Executorservice wird gekillt");
        es.shutdownNow();
        try {
          System.out.println("Executorservice kill wird erwartet.");
          es.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
        System.out.println("Executorservice wurde gekillt.");
      }
    };
    Runtime.getRuntime().addShutdownHook(testThread);
    es.submit(ss::accept);
  }
}
