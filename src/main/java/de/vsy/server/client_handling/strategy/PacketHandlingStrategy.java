package de.vsy.server.client_handling.strategy;

@FunctionalInterface
public interface PacketHandlingStrategy {

    /**
     * Command.
     */
    void administerStrategy();
}
