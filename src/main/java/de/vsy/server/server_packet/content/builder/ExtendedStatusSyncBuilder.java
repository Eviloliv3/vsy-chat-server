package de.vsy.server.server_packet.content.builder;

import static java.util.Set.copyOf;

import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import de.vsy.server.server_packet.content.ExtendedStatusSyncDTO;
import java.util.HashSet;
import java.util.Set;

/**
 * The Class ExtendedStatusSyncBuilder.*/
@JsonPOJOBuilder
public class ExtendedStatusSyncBuilder<T extends ExtendedStatusSyncBuilder<T>> extends
    SimpleStatusSyncBuilder<T> {

  private final Set<Integer> contactIdSet = new HashSet<>();

  public Set<Integer> getContactIdSet() {
    return copyOf(this.contactIdSet);
  }

  /**
   * With contacts.
   *
   * @param contactIds the contact ids
   * @return the extended status sync builder
   */
  public ExtendedStatusSyncBuilder<T> withContactIdSet(final Set<Integer> contactIds) {

    if (contactIds != null) {
      this.contactIdSet.addAll(contactIds);
    }
    return getInstanciable();
  }

  @Override
  public ExtendedStatusSyncBuilder<T> getInstanciable() {
    return this;
  }

  @Override
  public ExtendedStatusSyncDTO build() {
    return new ExtendedStatusSyncDTO(this);
  }
}
