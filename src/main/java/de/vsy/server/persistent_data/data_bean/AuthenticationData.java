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
import java.util.Optional;

/**
 * The Class Account.
 */
@JsonTypeName("communicatorData")
public class AuthenticationData implements Serializable {

  @Serial
  private static final long serialVersionUID = -697983373949759504L;
  private final int clientId;
  private final String username;
  private final String password;

  /**
   * Instantiates a new account.
   *
   * @param username    the username
   * @param password the password
   * @param clientId the client id
   */
  private AuthenticationData(final String username, final String password, final int clientId) {
    this.username = username;
    this.password = password;
    this.clientId = clientId;
  }

  /**
   * @param username    the username
   * @param password the password
   * @param clientId the clientId
   * @return new AuthenticationData object
   * @throws IllegalArgumentException if any input fails the corresponding check
   */
  @JsonCreator
  public static AuthenticationData valueOf(@JsonProperty("username") final String username,
      @JsonProperty("password") final String password,
      @JsonProperty("clientId") final int clientId) {
    Optional<String> checkString;

    if (username == null) {
      throw new IllegalArgumentException("No username specified.");
    }

    if (password == null) {
      throw new IllegalArgumentException("No password specified.");
    }

    checkString = IdCheck.checkData(clientId);
    if (checkString.isPresent()) {
      throw new IllegalArgumentException("Invalid client id: " + checkString.get());
    }

    return new AuthenticationData(username, password, clientId);
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
  public boolean equals(final Object otherObject) {
    if (this == otherObject) {
      return true;
    }

    if (otherObject instanceof AuthenticationData otherAccount) {
      return this.clientId == otherAccount.getClientId() && this.username.equals(
          otherAccount.getUsername())
          && this.password.equals(otherAccount.getPassword());
    }
    return false;
  }

  /**
   * Returns the client id.
   *
   * @return int
   */
  public int getClientId() {
    return this.clientId;
  }

  /**
   * Returns the username.
   *
   * @return the username
   */
  public String getUsername() {
    return this.username;
  }

  /**
   * Returns the password.
   *
   * @return the password
   */
  public String getPassword() {
    return this.password;
  }

  @Override
  public String toString() {
    return "\"clientId\": " + this.clientId + ", \"username\": " + this.username + ", \"password\": "
        + this.password;
  }

  /**
   * Same credentials.
   *
   * @param otherAuthData the other auth dataManagement
   * @return true, if successful
   */
  public boolean sameCredentials(final AuthenticationData otherAuthData) {

    if (otherAuthData != null) {
      return sameLogin(otherAuthData) && password.equals(otherAuthData.getPassword());
    }
    return false;
  }

  /**
   * Checks if two AuthenticationData bean contain the same username.
   *
   * @param otherAuthData the other auth dataManagement
   * @return boolean: true, if both bean contain the same username; false otherwise
   */
  public boolean sameLogin(final AuthenticationData otherAuthData) {

    if (otherAuthData != null) {
      return getUsername().equals(otherAuthData.getUsername());
    }
    return false;
  }
}
