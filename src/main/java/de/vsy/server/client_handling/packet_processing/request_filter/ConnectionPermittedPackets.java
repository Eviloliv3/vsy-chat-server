package de.vsy.server.client_handling.packet_processing.request_filter;

import static de.vsy.shared_transmission.packet.property.packet_category.PacketCategory.AUTHENTICATION;
import static de.vsy.shared_transmission.packet.property.packet_category.PacketCategory.ERROR;
import static java.util.List.of;

import de.vsy.shared_transmission.packet.property.packet_category.PacketCategory;
import java.util.List;

/**
 * The Class ConnectionPermittedPackets.
 */
public class ConnectionPermittedPackets extends PermittedPackets {

  /**
   * Instantiates a new connection permitted Packet.
   */
  public ConnectionPermittedPackets() {
    this(null);
  }

  /**
   * Instantiates a new connection permitted Packet.
   *
   * @param otherCategories the other categories
   */
  public ConnectionPermittedPackets(final PermittedCategoryProvider otherCategories) {
    super(getPermittedCategories(), otherCategories);
  }

  /**
   * Returns the permitted categories.
   *
   * @return the permitted categories
   */
  private static List<PacketCategory> getPermittedCategories() {
    return of(AUTHENTICATION, ERROR);
  }
}
