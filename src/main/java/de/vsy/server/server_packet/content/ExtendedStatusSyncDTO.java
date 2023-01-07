package de.vsy.server.server_packet.content;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.vsy.server.server_packet.content.builder.ExtendedStatusSyncBuilder;

import java.io.Serial;
import java.util.Set;

import static java.util.Set.copyOf;

/**
 * Contains state that shall be transferred to other connected servers, as well as clients
 */
@JsonDeserialize(builder = ExtendedStatusSyncBuilder.class)
public class ExtendedStatusSyncDTO extends BaseStatusSyncDTO implements ClientContactSync {

    @Serial
    private static final long serialVersionUID = -2447318735172645953L;
    private Set<Integer> contactIdSet;

    /**
     * Instantiates a new extended status sync dataManagement.
     *
     * @param builder the builder
     */
    public ExtendedStatusSyncDTO(final ExtendedStatusSyncBuilder<?> builder) {
        super(builder);
        this.contactIdSet = builder.getContactIdSet();
    }

    @Override
    public Set<Integer> getContactIdSet() {
        return copyOf(this.contactIdSet);
    }

    @Override
    public void setRemainingContactIds(final Set<Integer> remainingContacts) {
        this.contactIdSet = copyOf(remainingContacts);
    }

    @Override
    public String toString() {
        final var objectString = new StringBuilder();
        final var contactIListString = new StringBuilder();

        if (this.contactIdSet != null) {

            for (final var contactId : this.contactIdSet) {
                contactIListString.append(contactId).append(", ");
            }
        }

        if (contactIListString.length() >= 3) {
            contactIListString.setLength(contactIListString.length() - 2);
        } else {
            contactIListString.append("none");
        }
        objectString.append("\"extendedStatus\": { ").append(super.toString())
                .append(", \"contactIList\": [ ")
                .append(contactIListString).append(" ]").append(" }");
        return objectString.toString();
    }
}
