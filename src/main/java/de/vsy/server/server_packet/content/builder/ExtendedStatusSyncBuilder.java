package de.vsy.server.server_packet.content.builder;

import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import de.vsy.server.server_packet.content.ExtendedStatusSyncDTO;

import java.util.HashSet;
import java.util.Set;

import static java.util.Set.copyOf;

/**
 * The Class ExtendedStatusSyncBuilder.
 *
 * @author Frederic Heath
 */
@JsonPOJOBuilder
public
class ExtendedStatusSyncBuilder<T extends ExtendedStatusSyncBuilder<T>>
        extends SimpleStatusSyncBuilder<T> {

    private Set<Integer> contactIdSet = new HashSet<>();

    public
    Set<Integer> getContactIdList () {
        return copyOf(this.contactIdSet);
    }

    /**
     * With contacts.
     *
     * @param contactIds the contact ids
     *
     * @return the extended status sync builder
     */
    public
    ExtendedStatusSyncBuilder<T> withContactSet (final Set<Integer> contactIds) {

        if (contactIds != null) {
            this.contactIdSet = copyOf(contactIds);
        }
        return getInstanciable();
    }

    @Override
    public
    ExtendedStatusSyncBuilder<T> getInstanciable () {
        return this;
    }

    @Override
    public
    ExtendedStatusSyncDTO build () {

        if (this.contactIdSet == null) {
            this.contactIdSet = new HashSet<>();
        }
        return new ExtendedStatusSyncDTO(this);
    }
}
