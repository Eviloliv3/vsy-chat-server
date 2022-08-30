package de.vsy.server.server_packet.content;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.vsy.server.server_packet.content.builder.ExtendedStatusSyncBuilder;

import java.io.Serial;
import java.util.Set;

import static java.util.Set.copyOf;

/**
 * Wird versandt, wenn andere Klienten über einen Zustandswechsel eines Klienten
 * informiert werden müssen.
 */
@JsonDeserialize(builder = ExtendedStatusSyncBuilder.class)
public
class ExtendedStatusSyncDTO extends SimpleStatusSyncDTO
        implements ClientContactSync {

    @Serial
    private static final long serialVersionUID = -2447318735172645953L;
    private Set<Integer> contactIdList;

    /**
     * Instantiates a new extended status sync dataManagement.
     *
     * @param builder the builder
     */
    public
    ExtendedStatusSyncDTO (final ExtendedStatusSyncBuilder<?> builder) {
        super(builder);
        this.contactIdList = builder.getContactIdList();
    }

    @Override
    public
    Set<Integer> getContactIdList () {
        return copyOf(this.contactIdList);
    }

    @Override
    public
    void setRemainingContactIds (final Set<Integer> remainingContacts) {
        this.contactIdList = copyOf(remainingContacts);
    }

    @Override
    public
    String toString () {
        final var objectString = new StringBuilder();
        final var contactIListString = new StringBuilder();

        if (this.contactIdList != null) {

            for (final var contactId : this.contactIdList) {
                contactIListString.append(contactId).append(", ");
            }
        }

        if (contactIListString.length() >= 3) {
            contactIListString.setLength(contactIListString.length() - 2);
        } else {
            contactIListString.append("none");
        }
        objectString.append("\"extendedStatus\": { ")
                    .append(super.toString())
                    .append(", \"contactIList\": [ ")
                    .append(contactIListString)
                    .append(" ]")
                    .append(" }");
        return objectString.toString();
    }
}
