package org.folio.tm.integration.keycloak;

import static org.folio.tools.store.utils.SecretGenerator.generateSecret;

import lombok.RequiredArgsConstructor;
import org.folio.security.integration.keycloak.service.SecureStoreKeyProvider;
import org.folio.tm.integration.keycloak.configuration.KeycloakRealmSetupProperties;
import org.folio.tools.store.SecureStore;

@RequiredArgsConstructor
public class ClientSecretService {

  private final SecureStore secureStore;
  private final SecureStoreKeyProvider secureStoreKeyProvider;
  private final KeycloakRealmSetupProperties setupProperties;

  public String getOrCreateClientSecret(String realm, String clientId) {
    var key = secureStoreKeyProvider.tenantStoreKey(realm, clientId);
    return secureStore.lookup(key)
      .orElseGet(() -> generateAndSaveSecret(key));
  }

  private String generateAndSaveSecret(String key) {
    var secret = generateSecret(setupProperties.getClientSecretLength());
    secureStore.set(key, secret);
    return secret;
  }
}
