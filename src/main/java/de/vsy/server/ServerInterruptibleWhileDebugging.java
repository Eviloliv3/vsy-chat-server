package de.vsy.server;

import static de.vsy.shared_utility.standard_value.ThreadContextValues.LOG_ROUTE_CONTEXT_KEY;
import static de.vsy.shared_utility.standard_value.ThreadContextValues.STANDARD_SERVER_ROUTE_VALUE;

import de.vsy.shared_utility.logging.ThreadContextRunnable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

public class ServerInterruptibleWhileDebugging extends ChatServer implements Runnable{
  private static final Logger LOGGER = LogManager.getLogger();

  public static void main(String[] args){
    Thread serverThread = new Thread(new ServerInterruptibleWhileDebugging());
    Thread.currentThread().setName("Chatserver");
    ThreadContext.put(LOG_ROUTE_CONTEXT_KEY, STANDARD_SERVER_ROUTE_VALUE);
    Thread serverInterruptor = new Thread(new ThreadContextRunnable() {
      @Override
      protected void runWithContext() {
        try {
          Thread.sleep(5000);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
        serverThread.interrupt();System.out.println("Deed is done.");}

    }
    );

    serverThread.start();
    serverInterruptor.start();
    try {
      Thread.sleep(100000);
      serverThread.join();
      serverInterruptor.join();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    ThreadContext.clearAll();
  }

  @Override
  public void run() {
    final var server = new ChatServer();
    Thread.currentThread().setName("Chatserver");
    ThreadContext.put(LOG_ROUTE_CONTEXT_KEY, STANDARD_SERVER_ROUTE_VALUE);
    server.serve();
    LOGGER.trace("Server will be shutdown regularly.");
    server.shutdownServer();
    ThreadContext.clearAll();
  }
}
