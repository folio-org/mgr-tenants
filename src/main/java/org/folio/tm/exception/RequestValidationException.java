package org.folio.tm.exception;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.folio.common.domain.model.error.ErrorCode.VALIDATION_ERROR;

import java.io.Serial;
import java.util.List;
import lombok.Getter;
import org.folio.common.domain.model.error.ErrorCode;
import org.folio.common.domain.model.error.Parameter;

@Getter
public class RequestValidationException extends RuntimeException {

  @Serial private static final long serialVersionUID = -9043220455126894295L;

  private final List<Parameter> errorParameters;
  private final ErrorCode errorCode;

  /**
   * Creates {@link RequestValidationException} object for given message, key and value.
   *
   * @param message - validation error message
   * @param key     - validation key as field or parameter name
   * @param value   - invalid parameter value
   */
  public RequestValidationException(String message, String key, Object value) {
    super(message);

    this.errorCode = VALIDATION_ERROR;
    this.errorParameters = singletonList(new Parameter().key(key).value(String.valueOf(value)));
  }

  /**
   * Creates {@link RequestValidationException} object for given message.
   *
   * @param message - validation error message
   */
  public RequestValidationException(String message) {
    super(message);

    this.errorCode = VALIDATION_ERROR;
    this.errorParameters = emptyList();
  }
}
