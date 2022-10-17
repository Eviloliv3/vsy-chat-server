/*
 *
 */
package de.vsy.server.persistent_data.data_bean;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import de.vsy.shared_module.shared_module.data_element_validation.IdCheck;
import java.io.Serial;
import java.io.Serializable;
import java.util.Optional;

/**
 * The Class Account. Frederic Heath
 */
@JsonTypeName("communicatorData")
public class AuthenticationData implements Serializable {

  @Serial
  private static final long serialVersionUID = -697983373949759504L;
  private final int clientId;
  private final String login;
  private final String password;

  /**
   * Instantiates a new account.
   *
   * @param login    the login name
   * @param password the password
   * @param clientId the client id
   */
  private AuthenticationData(final String login, final String password, final int clientId) {
    this.login = login;
    this.password = password;
    this.clientId = clientId;
  }

  /**
   * @param login    the login
   * @param password the password
   * @param clientId the clientId
   * @return new AuthenticationData object
   * @throws IllegalArgumentException if any input fails the corresponding check
   */
  @JsonCreator
  public static AuthenticationData valueOf(@JsonProperty("login") final String login,
      @JsonProperty("password") final String password,
      @JsonProperty("clientId") final int clientId) {
    Optional<String> checkString;

    if (login == null) {
      throw new IllegalArgumentException("Kein Anzeigename (null).");
    }

    if (password == null) {
      throw new IllegalArgumentException("Kein Passwort (null).");
    }

    checkString = IdCheck.checkData(clientId);
    if (checkString.isPresent()) {
      throw new IllegalArgumentException("Ungültige Klienten-Id: " + checkString.get());
    }

    return new AuthenticationData(login, password, clientId);
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
      return this.clientId == otherAccount.getClientId() && this.login.equals(
          otherAccount.getLogin())
          && this.password.equals(otherAccount.getPassword());
    }
    return false;
  }

  /**
   * Gibt die Klienten-Id aus.
   *
   * @return die Klienten-Id
   */
  public int getClientId() {
    return clientId;
  }

  /**
   * Gets the login name.
   *
   * @return the login
   */
  public String getLogin() {
    return login;
  }

  /**
   * Gets the password.
   *
   * @return the password
   */
  public String getPassword() {
    return password;
  }

  @Override
  public String toString() {
    return "\"clientId\": " + this.clientId + ", \"login\": " + login + ", \"password\": "
        + password;
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
   * Checks if two AuthenticationData bean contains the same login name.
   *
   * @param otherAuthData the other auth dataManagement
   * @return boolean: true, if both bean contain the same login name; false otherwise
   */
  public boolean sameLogin(final AuthenticationData otherAuthData) {

    if (otherAuthData != null) {
      return getLogin().equals(otherAuthData.getLogin());
    }
    return false;
  }
}
