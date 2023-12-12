package org.folio.tm.integration.keycloak;

import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.tm.integration.keycloak.exception.KeycloakException;

@Log4j2
@RequiredArgsConstructor
public class KeycloakTemplate {

  private final TokenService tokenService;

  public void call(KeycloakMethod method, Supplier<String> expMsgSupplier) {
    call(token -> {
      method.call(token);
      return null;
    }, expMsgSupplier);
  }

  public <T> T call(KeycloakFunction<T> func, Supplier<String> expMsgSupplier) {
    try {
      return func.call(getToken());
    } catch (KeycloakException e) {
      throw e;
    } catch (Exception cause) {
      throw new KeycloakException(expMsgSupplier.get(), cause);
    }
  }

  private String getToken() {
    return tokenService.issueToken();
  }

  public interface KeycloakMethod {

    void call(String token);
  }

  public interface KeycloakFunction<T> {

    T call(String token);
  }
}
