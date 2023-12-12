package org.folio.tm.integration.keycloak;

import static org.apache.commons.lang3.StringUtils.stripToNull;
import static org.folio.security.integration.keycloak.utils.KeycloakSecretUtils.globalStoreKey;

import java.util.HashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.security.integration.keycloak.configuration.properties.KeycloakAdminProperties;
import org.folio.tools.store.SecureStore;
import org.folio.tools.store.exception.NotFoundException;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;

@Log4j2
@RequiredArgsConstructor
public class TokenService {

  private final KeycloakClient keycloakClient;
  private final KeycloakAdminProperties keycloakAdminProperties;
  private final SecureStore secureStore;

  @Cacheable(cacheNames = "token", key = "'admin-cli-token'")
  public String issueToken() {
    return requestToken();
  }

  @CachePut(cacheNames = "token", key = "'admin-cli-token'")
  public String renewToken() {
    return requestToken();
  }

  private String requestToken() {
    var loginRequest = new HashMap<String, String>();
    var clientId = keycloakAdminProperties.getClientId();

    String secret = null;
    try {
      secret = secureStore.get(globalStoreKey(clientId));
    } catch (NotFoundException e) {
      log.debug("Secret for 'admin' client is not defined in the secret store: clientId = {}", clientId);
    }

    loginRequest.put("client_id", clientId);
    loginRequest.put("client_secret", stripToNull(secret));
    loginRequest.put("username", stripToNull(keycloakAdminProperties.getUsername()));
    loginRequest.put("password", stripToNull(keycloakAdminProperties.getPassword()));
    loginRequest.put("grant_type", keycloakAdminProperties.getGrantType());

    log.info("Issuing access token for Keycloak communication [clientId: {}]", clientId);
    var token = keycloakClient.login(loginRequest);
    return token.getTokenType() + " " + token.getAccessToken();
  }
}
