package org.folio.tm.integration.keycloak.exception;

@SuppressWarnings("unused")
public class KeycloakException extends RuntimeException {

  public KeycloakException(String message) {
    super(message);
  }

  /**
   * Creates a new {@link KeycloakException} with message and cause for failed communication with keycloak.
   *
   * @param message - error message as {@link String} object
   * @param cause - error cause as {@link Throwable} object
   */
  public KeycloakException(String message, Throwable cause) {
    super(message, cause);
  }
}
