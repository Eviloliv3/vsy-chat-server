package de.vsy.server.client_handling.packet_processing.request_filter;

import static de.vsy.shared_transmission.shared_transmission.packet.property.packet_category.PacketCategory.CHAT;
import static java.util.List.of;

import de.vsy.shared_transmission.shared_transmission.packet.property.packet_category.PacketCategory;
import java.util.List;

/**
 * The Class ChatActivePermittedPackets.
 */
public class ActiveMessengerPermittedPackets extends PermittedPackets {

  /**
   * Instantiates a new chat active permitted Packet.
   */
  public ActiveMessengerPermittedPackets() {
    this(null);
  }

  /**
   * Instantiates a new chat active permitted Packet.
   *
   * @param otherCategories the other categories
   */
  public ActiveMessengerPermittedPackets(final PermittedCategoryProvider otherCategories) {
    super(getPermittedCategories(), otherCategories);
  }

  /**
   * Gets the permitted categories.
   *
   * @return the permitted categories
   */
  private static List<PacketCategory> getPermittedCategories() {
    return of(CHAT);
  }
}
