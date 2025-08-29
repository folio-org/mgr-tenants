package org.folio.tm.integration.keycloak;

import static org.folio.security.integration.keycloak.utils.KeycloakSecretUtils.tenantStoreKey;
import static org.folio.tools.store.utils.SecretGenerator.generateSecret;

import lombok.RequiredArgsConstructor;
import org.folio.tm.integration.keycloak.configuration.KeycloakRealmSetupProperties;
import org.folio.tools.store.SecureStore;

@RequiredArgsConstructor
public class ClientSecretService {

  private final SecureStore secureStore;
  private final KeycloakRealmSetupProperties setupProperties;

  public String getOrCreateClientSecret(String realm, String clientId) {
    var key = tenantStoreKey(realm, clientId);
    return secureStore.lookup(key)
      .orElseGet(() -> generateAndSaveSecret(key));
  }

  private String generateAndSaveSecret(String key) {
    var secret = generateSecret(setupProperties.getClientSecretLength());
    secureStore.set(key, secret);
    return secret;
  }
}
