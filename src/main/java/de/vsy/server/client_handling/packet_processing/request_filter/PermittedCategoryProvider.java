package de.vsy.server.client_handling.packet_processing.request_filter;

import de.vsy.shared_transmission.shared_transmission.packet.property.packet_category.PacketCategory;
import java.util.List;

/**
 * The Class PermittedCategoryProvider.
 */
public abstract class PermittedCategoryProvider {

  protected final PermittedCategoryProvider otherPermittedCategories;

  /**
   * Instantiates a new permitted category provider.
   */
  protected PermittedCategoryProvider() {
    this.otherPermittedCategories = null;
  }

  /**
   * Instantiates a new permitted category provider.
   *
   * @param otherCategories the other categories
   */
  protected PermittedCategoryProvider(final PermittedCategoryProvider otherCategories) {
    this.otherPermittedCategories = otherCategories;
  }

  /**
   * Gets the permitted Packetcategories.
   *
   * @return the permitted categories
   */
  public abstract List<PacketCategory> getPermittedPacketCategories();
}
