package org.folio.tm.integration.keycloak;

import static java.lang.String.format;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.tm.integration.keycloak.KeycloakTemplate.KeycloakFunction;
import org.folio.tm.integration.keycloak.model.AuthorizationClientPolicy;
import org.folio.tm.integration.keycloak.model.AuthorizationRolePolicy;

@Log4j2
@RequiredArgsConstructor
public class KeycloakPolicyService {

  private final KeycloakClient keycloakClient;
  private final KeycloakTemplate template;

  public AuthorizationRolePolicy createRolePolicy(AuthorizationRolePolicy policy, String realm, String clientId) {
    return template.call(createAuthRolePolicy(policy, realm, clientId),
      () -> format("Failed to create [%s] - based policy [%s] in client [%s]", policy.getType(), policy.getName(),
        clientId));
  }

  public AuthorizationClientPolicy createClientPolicy(AuthorizationClientPolicy policy, String realm, String clientId) {
    return template.call(createAuthClientPolicy(policy, realm, clientId),
      () -> format("Failed to create [%s] - based policy [%s] in client [%s]", policy.getType(), policy.getName(),
        clientId));
  }

  private KeycloakFunction<AuthorizationRolePolicy> createAuthRolePolicy(AuthorizationRolePolicy policy, String realm,
                                                                 String clientId) {
    return token -> {
      var res = keycloakClient.createRolePolicy(realm, clientId, policy, token);
      log.info("Keycloak System {} policy created with id: {}", res.getType(), res.getId());
      return res;
    };
  }

  private KeycloakFunction<AuthorizationClientPolicy> createAuthClientPolicy(AuthorizationClientPolicy policy,
                                                                          String realm,
                                                                 String clientId) {
    return token -> {
      var res = keycloakClient.createClientPolicy(realm, clientId, policy, token);
      log.info("Keycloak System {} policy created with id: {}", res.getType(), res.getId());
      return res;
    };
  }
}
