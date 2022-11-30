/*
 *
 */
package de.vsy.server.persistent_data.data_bean;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import de.vsy.shared_module.data_element_validation.IdCheck;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * The Class Contact.
 */
@JsonTypeName("communicatorData")
public class CommunicatorData implements Serializable {

  @Serial
  private static final long serialVersionUID = 3084589666217576189L;
  private final int communicatorId;
  private final String displayName;
  private final int ownerId;

  public CommunicatorData(CommunicatorData toCopy) {
    this(toCopy.getCommunicatorId(), toCopy.getOwnerId(), toCopy.getDisplayName());
  }

  /**
   * Instantiates a new active member.
   *
   * @param communicatorId the client id
   * @param ownerId        the owner id
   * @param displayName    the display name
   */
  private CommunicatorData(final int communicatorId, final int ownerId, final String displayName) {
    this.communicatorId = communicatorId;
    this.ownerId = ownerId;
    this.displayName = displayName;
  }

  @JsonCreator
  public static CommunicatorData valueOf(@JsonProperty("communicatorId") final int communicatorId,
      @JsonProperty("ownerId") final int ownerId,
      @JsonProperty("displayName") final String displayName) {

    if (IdCheck.checkData(communicatorId).isPresent() || IdCheck.checkData(ownerId).isPresent()) {
      throw new IllegalArgumentException("Invalid id: " + communicatorId + " or " + ownerId);
    }

    if (displayName == null) {
      throw new IllegalArgumentException("No display name.");
    }
    return new CommunicatorData(communicatorId, ownerId, displayName);
  }

  /**
   * Returns the entity id.
   *
   * @return int
   */
  public int getCommunicatorId() {
    return communicatorId;
  }

  /**
   * Returns the owner id.
   *
   * @return int
   */
  public int getOwnerId() {
    return ownerId;
  }

  /**
   * Returns the display label.
   *
   * @return String
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Checks if is owner.
   *
   * @param ownerId int
   * @return true, if specified id is owner
   */
  public boolean isOwner(final int ownerId) {
    return this.ownerId == ownerId;
  }

  @Override
  public int hashCode() {
    var hashCode = 37;
    final var declaredFields = this.getClass().getDeclaredFields();

    for (var i = (declaredFields.length - 1); i >= 0; i--) {
      hashCode *= declaredFields[i].hashCode();
    }
    return hashCode;
  }

  @Override
  public boolean equals(final Object o) {

    if (o instanceof CommunicatorData otherComm) {
      return Objects.equals(this.displayName, otherComm.getDisplayName());
    }
    return false;
  }

  @Override
  public String toString() {
    return "\"communicatorId\": " + this.communicatorId + ", \"ownerId\": " + this.ownerId
        + ", \"displayName\": "
        + this.displayName;
  }
}
