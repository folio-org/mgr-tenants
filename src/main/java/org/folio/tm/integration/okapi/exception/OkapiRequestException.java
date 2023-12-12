package org.folio.tm.integration.okapi.exception;

public class OkapiRequestException extends RuntimeException {

  /**
   * Creates a new {@link OkapiRequestException} with message and cause for failed communication with Okapi.
   *
   * @param message - error message as {@link String} object
   * @param cause - error cause as {@link Throwable} object
   */
  public OkapiRequestException(String message, Throwable cause) {
    super(message, cause);
  }
}
